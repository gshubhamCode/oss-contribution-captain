package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.constants.Ollama;
import org.fa.oss.contribution.helper.dto.request.OllamaRequest;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.fa.oss.contribution.helper.dto.response.OllamaResponse;
import org.fa.oss.contribution.helper.model.IssueSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class SummaryGenerator {

  private WebClient webClient =
      WebClient.builder()
          .baseUrl(Ollama.URL_AND_PORT)
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .build();

  @Autowired private IssuesService issuesService;

  @Autowired private ObjectMapper objectMapper;

  public IssueSummaryResultListDTO generateSummaries(List<IssueDTO> issueDTOS) throws IOException {
    List<IssueSummary> summaries =
        issueDTOS.stream().map(issue -> generateIssueSummary(issue)).collect(Collectors.toList());
    return IssueSummaryResultListDTO.builder().summaries(summaries).count(summaries.size()).build();
  }

  private IssueSummary generateIssueSummary(IssueDTO issue) {

    String prompt = preparePrompt(issue);
    OllamaRequest request =
        OllamaRequest.builder().model(Ollama.MODEL).prompt(prompt).stream(false).build();

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
      log.debug(objectMapper.writeValueAsString(ollamaResponse));
      return IssueSummary.builder()
          .issueDTO(issue)
          .summary(ollamaResponse.getResponse())
          .languages(new ArrayList<>())
          .build();
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse Ollama response", e);
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
      throw new RuntimeException("Failed to write summaries to file", e);
    }
  }

  private String getPromptTemplate() {
    return """
        You are a helpful assistant summarizing GitHub issues for contributors.
        Do not add your comments in the beginning like "the code summary is" or "Here is the summary of issue"

        Summarize the following issue and make sure to have detailed info for each section and a line gap.
        Keep formatting as shown below, summary should cover:

        * Main request or goal - 
        * Any validation rules or requirements - 
        * Attempted fixes or blockers (if mentioned) - 
        * Other notes - 

        GitHub Issue:

        """;
  }
}
