package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.config.GithubConfig;
import org.fa.oss.contribution.helper.constants.Github;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IssuesService {

  @Autowired GithubConfig githubConfig;

  @Autowired ObjectMapper objectMapper;

  private static final String ISSUES_FILENAME = "issues.json";
  private static final int CACHE_VALIDITY_MINUTES = 60 * 24 * 365;

  public List<IssueDTO> searchGoodFirstIssues() throws IOException {
    GitHub github = new GitHubBuilder().withOAuthToken(githubConfig.getGithubToken()).build();

    PagedSearchIterable<GHIssue> results =
        github
            .searchIssues()
            .q("label:\"good first issue\" state:open is:issue")
            .sort(GHIssueSearchBuilder.Sort.UPDATED)
            .order(GHDirection.DESC)
            .list();

    List<GHIssue> issuesList = results.withPageSize(Github.PAGE_SIZE).toList();

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
      List<IssueDTO> issues = searchGoodFirstIssues();
      saveIssues(issues);
      return issues;
    } catch (IOException e) {
      throw new RuntimeException("Failed to fetch issues from GitHub", e);
    }
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

  private boolean isFileOlderThan(File file, int minutes) {
    long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes);
    return file.lastModified() < cutoff;
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
      return null;
    }
  }
}
