package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.constants.Ollama;
import org.fa.oss.contribution.helper.dto.request.OllamaRequest;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.fa.oss.contribution.helper.dto.response.OllamaResponse;
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

@Service
@Slf4j
public class SummaryGenerator {

  public int prompt_tokens;
  public int total_tokens;
  public int completion_tokens;

  private WebClient webClient =
      WebClient.builder()
          .baseUrl(Ollama.URL_AND_PORT)
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .exchangeStrategies(
              ExchangeStrategies.builder()
                  .codecs(
                      configurer ->
                          configurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)) // 100 MB
                  .build())
          .build();

  @Autowired private IssuesService issuesService;

  @Autowired private ObjectMapper objectMapper;

  public IssueSummaryResultListDTO generateSummaries(List<IssueDTO> issueDTOS) {
    try {
      List<IssueSummary> summaries =
          issueDTOS.parallelStream()
              .map(
                  issue -> {
                    try {
                      return generateIssueSummaryUsingOpenAI(issue);
                    } catch (Exception e) {
                      log.error("Skipping issue due to exception: {} {}", issue.getId(), issue.getTitle(), e);
                      return null; // or Optional.empty()
                    }
                  })
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

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
    } finally{
      log.info("prompt_tokens: " + prompt_tokens);
      log.info("completion_tokens: " + completion_tokens);
      log.info("total_tokens: " + total_tokens);
    }
  }

  private IssueSummary generateIssueSummary(IssueDTO issue) {

    String prompt = preparePrompt(issue);
    String format =
        "{ \"type\": \"object\", \"properties\": { \"main\": { \"type\": \"string\", \"description\": \"A concise summary of the issue or bug being reported.\" }, \"validationOrRequirement\": { \"type\": \"string\", \"description\": \"The expected behavior or requirement that needs to be met.\" }, \"attemptedFixes\": { \"type\": \"string\", \"description\": \"Any attempted solutions or approaches that have been tried so far.\" }, \"otherNotes\": { \"type\": \"string\", \"description\": \"Additional context, labels, or contributor guidance related to the issue.\" } }, \"required\": [ \"main\", \"validationOrRequirement\", \"attemptedFixes\", \"otherNotes\" ] }";
    Map<String, Object> formatSchema = null;
    try {
      formatSchema = objectMapper.readValue(format, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      log.error("ollama response format key failed to parse to json", e);
      formatSchema = Map.of("type", "json");
    }
    OllamaRequest request =
        OllamaRequest.builder().model(Ollama.MODEL).prompt(prompt).stream(false)
            .format(formatSchema)
            .build();

    log.info("Generating summary for issue: " + issue.getId());
    String response =
        webClient
            .post()
            .uri(Ollama.GENERATE_ENDPOINT)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .block();

    try {
      objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
      OllamaResponse ollamaResponse = objectMapper.readValue(response, OllamaResponse.class);
      objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
      log.info(objectMapper.writeValueAsString(ollamaResponse));
      SummaryDTO summaryDTO;
      try {
        summaryDTO = objectMapper.readValue(ollamaResponse.getResponse(), SummaryDTO.class);
        summaryDTO.setValidJson(true);
        summaryDTO.setSummaryText("");
      } catch (JsonParseException e) {
        summaryDTO =
            SummaryDTO.builder().summaryText(ollamaResponse.getResponse()).validJson(false).build();
      }
      return IssueSummary.builder()
          .issueDTO(issue)
          .summary(summaryDTO)
          .updatedAt(ollamaResponse.getCreatedAt().toEpochSecond())
          .build();
    } catch (IOException e) {
      log.error("Failed to parse Ollama response", e);
      return IssueSummary.builder()
          .issueDTO(issue)
          .summary(SummaryDTO.builder().summaryText(response).validJson(false).build())
          .updatedAt(ZonedDateTime.now().toEpochSecond())
          .build();
    }
  }

  private IssueSummary generateIssueSummaryUsingOpenAI(IssueDTO issue) {

    String prompt = preparePrompt(issue);

    OpenAIChatCompletionRequest request = OpenAIChatCompletionRequest.getDefaultChatRequest(prompt);
    request.setModel("01-ai/Yi-34B-Chat");
    log.info("Generating summary for issue: " + issue.getId() + " title: " + issue.getTitle());

    String responseJson =
        webClient
            .post()
            .uri("https://gv1gj0u3l0jh1f-8000.proxy.runpod.net/v1/chat/completions")
            .header("Authorization", "Bearer sk-IrR7Bwxtin0haWagUnPrBgq5PurnUz86")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .block();

    try {
      // Parse the vLLM / OpenAI completion response
      OpenAIChatCompletionResponse completionResponse =
          objectMapper.readValue(responseJson, OpenAIChatCompletionResponse.class);
      log.info(completionResponse.getUsage().toString());

      // TO DO:: remove code
      OpenAIChatCompletionResponse.Usage usage = completionResponse.getUsage();
      prompt_tokens = Math.max(prompt_tokens, usage.getPrompt_tokens());
      total_tokens = Math.max(total_tokens, usage.getTotal_tokens());
      completion_tokens = Math.max(completion_tokens, usage.getCompletion_tokens());


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
      return IssueSummary.builder()
          .issueDTO(issue)
          .summary(SummaryDTO.builder().summaryText(responseJson).validJson(false).build())
          .updatedAt(Instant.now().getEpochSecond())
          .build();
    }
  }

  private String preparePrompt(IssueDTO issueDTO) {
    try {
      String issue = objectMapper.writeValueAsString(issueDTO);
      return getPromptTemplate() + issue;
    } catch (JsonProcessingException e) {
      log.debug("Json processing failed for " + issueDTO, e);
      return String.format(
          """
              %s
              - Title: %s
              - Description: %s
              - Labels: %s
              - Comments: %s
              """,
          getPromptTemplate(),
          issueDTO.getTitle(),
          issueDTO.getDescription(),
          String.join(", ", issueDTO.getLabels()),
          issueDTO.getComments());
    }
  }

  public void saveSummaries(IssueSummaryResultListDTO summaries) {
    try {
      String currentDir = System.getProperty("user.dir");
      String filePath = currentDir + File.separator + "summary.json";
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), summaries);
      System.out.println("Summaries saved to " + filePath);
    } catch (IOException e) {
      log.error("Failed to write summaries to file", e);
    }
  }

  private String getPromptTemplate() {
    return """
        You are a helpful assistant summarizing GitHub issues for contributors.
        Respond using JSON. Keep the format of your response as shown in below example else you will be heavily penalised!!!
        Do not add your comments in the beginning like "the code summary is" or "Here is the summary of issue" or "You want to understand" or "}Summary:"

        Below is a sample of summary generated for an issue.
        Example begins now
        {"main": "The logo at the Header component is currently off-center, affecting the overall visual alignment and aesthetics of the page. The issue needs to be fixed so that the logo is horizontally centered within the header across all screen sizes.","validationOrRequirement": "The expected behavior is for the logo to be visually centered horizontally across all screen sizes without breaking responsiveness or causing regression on other header elements.","attemptedFixes": "The fix can be implemented using Styled Components to adjust the CSS layout and ensure the logo is centered after the fix. Turning relative URLs into absolute URLs would also address the issue as noticed by user osandamaleesha in one usage-rules.md file.","otherNotes": "This issue is currently labeled as 'bug' and 'good first issue', indicating it's a significant issue suitable for a contributor to tackle. A pull request should be submitted targeting the main branch with before/after screenshots or video if possible."}
        Example ends here

        Summarize the following issue and make sure to have detailed info for each section. Do not add * in the beginning of each section else you will be heavily penalised !.
        summary should cover:

        Main-
        validationOrRequirement-
        attemptedFixes -
        otherNotes -


        GitHub Issue:

        """;
  }
}
