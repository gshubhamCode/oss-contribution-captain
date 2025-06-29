package org.fa.oss.contribution.helper.service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.cache.CentralCacheService;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.RepositoryDTO;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IssuesService {
  @Autowired GHIssueService ghIssueService;

  @Autowired RepositoryService repositoryService;

  @Autowired CentralCacheService centralCacheService;

  public List<IssueDTO> searchGoodFirstIssues() throws IOException {
    List<GHIssue> issuesList = ghIssueService.getGHIssues();
    return issuesList.parallelStream().map(this::mapIssueToDTO).filter(Objects::nonNull).toList();
  }

  public List<IssueDTO> getIssues() {

    List<IssueDTO> cachedIssues = centralCacheService.getIssueCache().load();
    if (cachedIssues != null) {
      log.info("Returning cached issues from cache");
      return cachedIssues;
    }
    return generateIssues();

  }

  public List<IssueDTO> generateIssues() {
    try {
      List<IssueDTO> issueDTOList = searchGoodFirstIssues();
      log.info("Fetching repository details of issues");
      Map<String, RepositoryDTO> repositories =
              repositoryService.mapToRepositoryDTO(repositoryService.getRepositoryForIssues());
      log.info("Fetch complete");


      log.info("Mapping repo in Issues complete");
      issueDTOList.forEach(
              issue -> issue.setRepository(repositories.get(issue.getRepositoryName())));

      log.info("Filtering issues from top repo");
      List<IssueDTO> filteredIssues =
              issueDTOList.parallelStream()
                      .filter(issueDTO -> Objects.nonNull(issueDTO.getRepository()))
                      .filter(
                              issue ->
                                      issue.getRepository().getStargazersCount() > 15
                                              && issue.getRepository().getForksCount() > 10)
                      .collect(Collectors.toList());
      log.info("Filter complete");

      log.info("Save Issues in cache");
      centralCacheService.getIssueCache().save(filteredIssues);
      log.info("Saved in cache");

      return filteredIssues;
    } catch (IOException e) {
      log.error("Failed to fetch issues from GitHub", e);
      return Collections.emptyList();
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
