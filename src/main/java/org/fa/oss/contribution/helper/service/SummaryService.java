package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
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

  private final WebClient webClient =
      WebClient.builder()
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .exchangeStrategies(
              ExchangeStrategies.builder()
                  .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(HUNDRED_MB))
                  .build())
          .build();

  public IssueSummaryResultListDTO generateSummaries(List<IssueDTO> issueDTOS) {
    final ExecutorService ioExecutor = Executors.newFixedThreadPool(8);
    ;
    try {
      runPodManager.startPod();
      runPodManager.waitForRunningPod();

      List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
      List<CompletableFuture<IssueSummary>> futures =
          issueDTOS.stream()
              .map(
                  issue ->
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              return generateIssueSummaryUsingOpenAI(
                                  issue); // makes an HTTP call, etc.
                            } catch (WebClientResponseException.BadRequest e) {
                              log.error(
                                  "400 Bad Request - likely context length exceeded: {}",
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

      return IssueSummaryResultListDTO.builder()
          .summaries(summaries)
          .count(summaries.size())
          .build();
    } catch (Exception e) {
      log.error("Error in generating summaries for issues using parallel stream", e);
      return IssueSummaryResultListDTO.builder()
          .summaries(Collections.emptyList())
          .count(Collections.emptyList().size())
          .build();
    } finally {
      // runPodManager.stopPod();
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

  public IssueSummaryResultListDTO generateSummaries(int limit) {
    try {
      runPodManager.startPod();
      runPodManager.waitForRunningPod();

      List<IssueDTO> issues;
      if (centralCacheService.getIssueCache() != null) {
        issues = centralCacheService.getIssueCache().load();
      } else {
        issues = issuesService.getIssues();
      }

      List<IssueDTO> filteredIssues =
          issues.parallelStream()
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
                        return  false;
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

      log.info("Generating summaries");
      List<IssueSummary> summaries =
          filteredIssues.parallelStream()
              .map(this::generateIssueSummaryUsingOpenAI)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

      IssueSummaryResultListDTO result =
          IssueSummaryResultListDTO.builder().summaries(summaries).count(summaries.size()).build();

      log.info("Saving summaries in cache");
      centralCacheService.getSummaryCache().save(result);
      log.info("Summaries saved in cache");
      return result;

    } catch (Exception e) {
      log.error("Failed to generate summaries", e);
      return IssueSummaryResultListDTO.builder().count(0).summaries(List.of()).build();
    } finally {
      runPodManager.stopPod();
    }
  }

  private IssueSummary generateIssueSummaryUsingOpenAI(IssueDTO issue) {
    try {

      String prompt = promptService.preparePrompt(issue);

      OpenAIChatCompletionRequest request =
          OpenAIChatCompletionRequest.getDefaultChatRequest(prompt);
      log.info("Generating summary for issue: " + issue.getId() + " title: " + issue.getTitle());
      String chatCompletionUrl =
          "https://" + runPodConfig.getPodId() + "-8000.proxy.runpod.net/v1/chat/completions";
      String responseJson =
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
      String generatedText =
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
      } catch (JsonParseException e) {
        summaryDTO = SummaryDTO.builder().summaryText(generatedText).validJson(false).build();
      }

      return IssueSummary.builder()
          .issueDTO(issue)
          .summary(summaryDTO)
          .updatedAt(Instant.now().getEpochSecond())
          .build();

    } catch (IOException e) {
      log.error("Failed to parse completion response", e);
      return null;
    }
  }

  private <T> Stream<T> maybeLimit(Stream<T> stream, int limit) {
    return limit > 0 ? stream.limit(limit) : stream;
  }
}
