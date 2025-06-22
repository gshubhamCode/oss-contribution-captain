package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.RepositoryDTO;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IssuesService {
  @Autowired ObjectMapper objectMapper;

  @Autowired GHIssueService ghIssueService;

  @Autowired RepositoryService repositoryService;

  private static final String ISSUES_FILENAME = "issues.json";
  private static final int CACHE_VALIDITY_MINUTES = 0;

  public List<IssueDTO> searchGoodFirstIssues() throws IOException {
    List<GHIssue> issuesList = ghIssueService.getGHIssues();
    return issuesList.parallelStream().map(this::mapIssueToDTO).filter(Objects::nonNull).toList();
  }

  public List<IssueDTO> getCachedOrFetchedIssues() {
    File issuesFile = new File(System.getProperty("user.dir") + File.separator + ISSUES_FILENAME);

    if (issuesFile.exists() && !isFileOlderThan(issuesFile, CACHE_VALIDITY_MINUTES)) {
      log.info("Returning cached issues from {}", ISSUES_FILENAME);
      try {
        return Arrays.asList(objectMapper.readValue(issuesFile, IssueDTO[].class));
      } catch (IOException e) {
        log.warn("Failed to read cached issues file, fetching new ones", e);
      }
    }

    try {
      List<GHIssue> issues = ghIssueService.getGHIssues();
      List<IssueDTO> issueDTOList = issues.stream().map(this::mapIssueToDTO).toList();
      Map<String, RepositoryDTO> repositories =
          repositoryService.mapToRepositoryDTO(repositoryService.getRepositoryForIssues());
      issueDTOList.forEach(
          issue -> issue.setRepository(repositories.get(issue.getRepositoryName())));
      List<IssueDTO> filteredIssues =
          issueDTOList.parallelStream()
                  .filter(issueDTO -> Objects.nonNull(issueDTO.getRepository()))
              .filter(
                  issue ->
                      issue.getRepository().getStargazersCount() > 15
                          && issue.getRepository().getForksCount() > 10)
              .collect(Collectors.toList());
      saveIssues(filteredIssues);
      return filteredIssues;
    } catch (IOException e) {
      throw new RuntimeException("Failed to fetch issues from GitHub", e);
    }
  }

  private boolean isFileOlderThan(File file, int minutes) {
    long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes);
    return file.lastModified() < cutoff;
  }

  public void saveIssues(List<IssueDTO> issueDTOS) {
    try {
      String currentDir = System.getProperty("user.dir");
      String filePath = currentDir + File.separator + ISSUES_FILENAME;
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), issueDTOS);
      System.out.println("Issues saved to " + filePath);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write issues to file", e);
    }
  }

  private IssueDTO mapIssueToDTO(GHIssue issue) {
    log.info("Mapping issue: {} {}", issue.getUrl(), issue.getTitle());
    String[] path = issue.getUrl().getPath().split("/");

    try {
      List<String> comments =
          issue.listComments().withPageSize(100).toList().parallelStream()
              .map(GHIssueComment::getBody)
              .collect(Collectors.toList());

      return IssueDTO.builder()
          .id(issue.getId())
          .url(issue.getHtmlUrl().toString())
          .title(issue.getTitle())
          .description(issue.getBody())
          .repositoryName(path[2] + "/" + path[3])
          .updatedAt(issue.getUpdatedAt())
          .user(issue.getUser().getLogin())
          .userHtmlUrl(issue.getUser().getHtmlUrl().toString())
          .userAvatarUrl(issue.getUser().getAvatarUrl())
          .labels(issue.getLabels().stream().map(GHLabel::getName).collect(Collectors.toSet()))
          .comments(comments)
          .state(issue.getState().toString())
          .build();

    } catch (IOException e) {
      log.debug("Error mapping GitHub Issue to DTO (ID: {})", issue.getId(), e);
      return IssueDTO.builder().build();
    }
  }
}
