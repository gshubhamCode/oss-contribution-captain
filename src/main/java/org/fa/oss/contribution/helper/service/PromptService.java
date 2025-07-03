package org.fa.oss.contribution.helper.service;

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
    return String.format(
        """
                            %s
                            - Title: %s
                            - Description: %s
                            - Labels: %s
                            - Comments: %s
                            - Author: %s
                            - Repository name: %s
              """,
        getPromptTemplate(),
        issueDTO.getTitle(),
        issueDTO.getDescription(),
        String.join(", ", issueDTO.getLabels()),
        issueDTO.getComments(),
        issueDTO.getUser(),
        issueDTO.getRepositoryName());
  }

  private String getPromptTemplate() {
    return """
        You are a helpful assistant summarizing GitHub issues for contributors.
        Please return the result in the exact JSON format below, without any extra commentary or markdown.

        Below is a sample structure of summary in json format generated for an issue, with little brief on what each json property defines mentioned in the value part of example.
        Note the values in below sample are not actual values, they are just the brief about property. You are expected to generate summary based on the definition provided and actual github issue.
        Example structure begins now
        {"main": "objective of the issue","validationOrRequirement": "any validations or specific requirements","attemptedFixes": "any foxes tried or any blockers encountered","otherNotes": "any other note specific to issue find in description or comments"}
        Example structure ends here

        Summarize the following issue and make sure to have detailed info for each section. Do not add * in the beginning of each section else you will be heavily penalised !.
        summary should cover:

        - main: summary of the issue's main goal
        - validationOrRequirement: validations or requirements
        - attemptedFixes: attempts or blockers
        - otherNotes: other relevant context from description or comments

        Do not include markdown, comments, or any text outside the JSON.

        GitHub Issue:

        """;
  }
}
