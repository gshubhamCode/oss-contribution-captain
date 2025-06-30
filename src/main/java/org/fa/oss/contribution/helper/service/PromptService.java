package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PromptService {

  @Autowired private ObjectMapper objectMapper;

  public String preparePrompt(IssueDTO issueDTO) {
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
