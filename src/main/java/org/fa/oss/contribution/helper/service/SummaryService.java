package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.cache.CentralCacheService;
import org.fa.oss.contribution.helper.config.RunPodConfig;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.fa.oss.contribution.helper.dto.response.SummaryDTO;
import org.fa.oss.contribution.helper.model.IssueSummary;
import org.fa.oss.contribution.helper.model.OpenAIChatCompletionRequest;
import org.fa.oss.contribution.helper.model.OpenAIChatCompletionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
public class SummaryService {

  @Autowired private RunPodManager runPodManager;
  @Autowired private RunPodConfig runPodConfig;
  @Autowired private IssuesService issuesService;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CentralCacheService centralCacheService;

  @Autowired private PromptService promptService;

  private static final int HUNDRED_MB = 100 * 1024 * 1024;

  private static final int CONTEXT_TOKEN_LIMIT = 8192;

  private String rawJson = "{}";

  private final WebClient webClient =
      WebClient.builder()
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .exchangeStrategies(
              ExchangeStrategies.builder()
                  .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(HUNDRED_MB))
                  .build())
          .build();

  @Autowired
  public SummaryService(
          RunPodManager runPodManager,
          RunPodConfig runPodConfig,
          IssuesService issuesService,
          ObjectMapper objectMapper,
          CentralCacheService centralCacheService,
          PromptService promptService
  ) {
    this.runPodManager = runPodManager;
    this.runPodConfig = runPodConfig;
    this.issuesService = issuesService;
    this.objectMapper = objectMapper;
    this.centralCacheService = centralCacheService;
    this.promptService = promptService;
    reloadRawSummary();
  }


  public IssueSummaryResultListDTO generateSummaries(List<IssueDTO> issueDTOS) {
    final ExecutorService ioExecutor = Executors.newFixedThreadPool(8);
    try {
      runPodManager.startPod();
      runPodManager.waitForRunningPod();
      List<IssueDTO> filteredIssues =
          issueDTOS.parallelStream()
              .filter(
                  issue -> {
                    try {
                      String prompt = promptService.preparePrompt(issue);
                      long tokenCount = TokenCounter.countTokens(prompt);
                      if (tokenCount < CONTEXT_TOKEN_LIMIT) {
                        return true;
                      } else {
                        log.warn(
                            "Token check failed. issue: {}, token count: {}, limit: {}",
                            issue.getUrl(),
                            tokenCount,
                            CONTEXT_TOKEN_LIMIT);
                        return false;
                      }
                    } catch (Exception e) {
                      log.error(
                          "Token check failed due to exception for issue {}: {}",
                          issue.getUrl(),
                          e.getMessage(),
                          e);
                      return false;
                    }
                  })
              .collect(Collectors.toList());

      List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
      List<CompletableFuture<IssueSummary>> futures =
          filteredIssues.stream()
              .map(
                  issue ->
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              return generateIssueSummaryUsingOpenAI(issue);
                            } catch (WebClientResponseException e) {
                              log.error(
                                  "Request Error - likely context length exceeded: {}",
                                  e.getResponseBodyAsString());
                              errors.add(e);
                              return null;
                            } catch (Exception e) {
                              errors.add(e);
                              log.error(
                                  "Skipping issue due to exception: {} {}",
                                  issue.getId(),
                                  issue.getTitle(),
                                  e);
                              return null;
                            }
                          },
                          ioExecutor))
              .collect(Collectors.toList());

      List<IssueSummary> summaries =
          futures.stream()
              .map(
                  future -> {
                    try {
                      return future.get(); // or future.join() if you prefer unchecked
                    } catch (Exception e) {
                      log.error("Error while generating summary", e);
                      return null;
                    }
                  })
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

      if (!errors.isEmpty()) {
        log.warn("Summary generation completed with {} errors", errors.size());
      }

      IssueSummaryResultListDTO result =
          IssueSummaryResultListDTO.builder().summaries(summaries).count(summaries.size()).build();
      log.info("Saving summaries in cache");
      centralCacheService.getSummaryCache().save(result);
      log.info("Summaries saved in cache");
      return result;

    } catch (Exception e) {
      log.error("Failed to generate summaries", e);
      return IssueSummaryResultListDTO.builder()
          .summaries(Collections.emptyList())
          .count(Collections.emptyList().size())
          .build();
    } finally {
      try {
        runPodManager.stopPod();
      } catch (Exception ex) {
        log.warn("Error stopping pod", ex);
      }
      if (ioExecutor != null) {
        ioExecutor.shutdown();
      }
    }
  }

  public IssueSummaryResultListDTO getSummaries(int limit) {
    IssueSummaryResultListDTO cached = centralCacheService.getSummaryCache().load();
    if (cached != null) {
      log.info("Loaded {} summaries from cache", cached.getCount());
      return cached;
    }
    return generateSummaries(limit);
  }

  private void reloadRawSummary() {
    try {
      if (new File(centralCacheService.getSummaryCache().filePath()).exists()) {
        rawJson = Files.readString(Path.of(centralCacheService.getSummaryCache().filePath()));
      }
    } catch (IOException e) {
      log.error("Summary load failed", e);
    }
  }

  public String generateSummariesRaw(int limit) throws IOException {
    return rawJson;
  }

  public IssueSummaryResultListDTO generateSummaries(int limit) {
    List<IssueDTO> issues;
    if (centralCacheService.getIssueCache() != null) {
      issues = centralCacheService.getIssueCache().load();
    } else {
      issues = issuesService.getIssues();
    }

    List<IssueDTO> filteredIssues = maybeLimit(issues.stream(), limit).collect(Collectors.toList());

    IssueSummaryResultListDTO issueSummaryResultListDTO =  generateSummaries(filteredIssues);
    reloadRawSummary();
    return issueSummaryResultListDTO;
  }

  private IssueSummary generateIssueSummaryUsingOpenAI(IssueDTO issue) {
    String generatedText = "";
    String responseJson = "";

    try {

      String prompt = promptService.preparePrompt(issue);

      OpenAIChatCompletionRequest request =
          OpenAIChatCompletionRequest.getDefaultChatRequest(prompt);
      log.info("Generating summary for issue: " + issue.getId() + " title: " + issue.getTitle());
      String chatCompletionUrl =
          "https://" + runPodConfig.getVllmPodId() + "-8000.proxy.runpod.net/v1/chat/completions";
      responseJson =
          webClient
              .post()
              .uri(chatCompletionUrl)
              .header("Authorization", "Bearer " + runPodConfig.getVllmApiKey())
              .bodyValue(request)
              .retrieve()
              .bodyToMono(String.class)
              .block();

      // Parse the vLLM / OpenAI completion response
      OpenAIChatCompletionResponse completionResponse =
          objectMapper.readValue(responseJson, OpenAIChatCompletionResponse.class);
      log.info(completionResponse.getUsage().toString());

      // Extract the generated text from response (usually in choices[0].text or .message.content)
      generatedText =
          completionResponse
              .getChoices()
              .get(0)
              .getMessage()
              .getTool_calls()
              .get(0)
              .function
              .arguments;

      // Parse generatedText as your expected JSON format (the same format schema you want)
      SummaryDTO summaryDTO;
      try {
        summaryDTO = objectMapper.readValue(generatedText, SummaryDTO.class);
        summaryDTO.setValidJson(true);
        summaryDTO.setSummaryText("");
      } catch (JsonParseException | JsonMappingException e) {
        log.error("Failed to parse generated summary text: {} error: {}", generatedText, e);
        return null;
      }

      return IssueSummary.builder()
          .issueDTO(issue)
          .summary(summaryDTO)
          .updatedAt(Instant.now().getEpochSecond())
          .build();

    } catch (Exception e) {
      log.error("Failed to parse completion response: {}  error: {}", responseJson, e);
      return null;
    }
  }

  private <T> Stream<T> maybeLimit(Stream<T> stream, int limit) {
    return limit > 0 ? stream.limit(limit) : stream;
  }
}
