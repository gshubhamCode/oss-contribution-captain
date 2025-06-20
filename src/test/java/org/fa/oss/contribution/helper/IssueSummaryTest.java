package org.fa.oss.contribution.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.fa.oss.contribution.helper.service.SummaryGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "github.token=dummy_test_token")
public class IssueSummaryTest {

  @Autowired ObjectMapper objectMapper;

  @Autowired SummaryGenerator summaryGenerator;

  @Autowired
  public void configureObjectMapper(ObjectMapper objectMapper) {
    objectMapper.registerModule(new JavaTimeModule());
  }

  private List<IssueDTO> loadIssues(String fileName) {
    try {
      // Load file from classpath (test/resources or main/resources)
      ClassLoader classLoader = getClass().getClassLoader();
      URL fileResource = classLoader.getResource(fileName);
      File file;
      if (fileResource != null) {
        file = new File(fileResource.getFile());
      } else {
        throw new FileNotFoundException(fileName + "Test data file is not available");
      }

      return objectMapper.readValue(
          file, objectMapper.getTypeFactory().constructCollectionType(List.class, IssueDTO.class));
    } catch (IOException e) {
      throw new RuntimeException("Failed to load issues from resource file: " + fileName, e);
    }
  }

  @Test
  public void test_1_issue() {
    String fileName = "issues_dump_1.json";
    List<IssueDTO> issues = loadIssues(fileName);
    try {
      IssueSummaryResultListDTO issueSummaryResultListDTO =
          summaryGenerator.generateSummaries(issues);
      summaryGenerator.saveSummaries(issueSummaryResultListDTO);
    } catch (IOException e) {
      Assertions.fail("Exception thrown while testing", e);
      throw new RuntimeException(e);
    }
  }

  @Test
  public void test_10_issue() {
    String fileName = "issues_dump_10.json";
    List<IssueDTO> issues = loadIssues(fileName);
    try {
      IssueSummaryResultListDTO issueSummaryResultListDTO =
          summaryGenerator.generateSummaries(issues);
      summaryGenerator.saveSummaries(issueSummaryResultListDTO);
    } catch (IOException e) {
      Assertions.fail("Exception thrown while testing", e);
      throw new RuntimeException(e);
    }
  }
}
