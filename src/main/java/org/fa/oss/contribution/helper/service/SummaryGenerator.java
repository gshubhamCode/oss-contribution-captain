package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.constants.Ollama;
import org.fa.oss.contribution.helper.dto.request.OllamaRequest;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.fa.oss.contribution.helper.dto.response.OllamaResponse;
import org.fa.oss.contribution.helper.dto.response.SummaryDTO;
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
        OllamaRequest.builder().model(Ollama.MODEL).prompt(prompt).format("json").stream(false).build();

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
      try{
        summaryDTO = objectMapper.readValue(ollamaResponse.getResponse(), SummaryDTO.class);
        summaryDTO.setValidJson( true);
        summaryDTO.setSummaryText("");
      }catch (JsonParseException e){
        summaryDTO = SummaryDTO.builder()
                .summaryText(ollamaResponse.getResponse())
                .validJson(false)
                .build();
      }
      return IssueSummary.builder()
          .issueDTO(issue)
          .summary(summaryDTO)
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

        Below is a sample of summary generated for an issue. Keep the format of your response as shown in below example else you will be heavily penalised!!!
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
