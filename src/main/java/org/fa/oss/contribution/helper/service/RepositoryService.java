package org.fa.oss.contribution.helper.service;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.dto.response.OwnerDTO;
import org.fa.oss.contribution.helper.dto.response.RepositoryDTO;
import org.kohsuke.github.GHRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RepositoryService {

  @Autowired GHIssueService ghIssueService;

  public Map<String, GHRepository> getRepositoryForIssues() throws IOException {
    Map<String, GHRepository> repoMap = new HashMap<>();

    ghIssueService
        .getGHIssues()
        .forEach(
            issue -> {
              String key = getRepositoryNameFromUrl(issue.getUrl());
              // Only call getRepository() if key is not in map or has null value
              try {
                repoMap.computeIfAbsent(
                    key,
                    k -> {
                      log.info("Fetch repo:{} details for issue:{} ", key, issue.getHtmlUrl());
                      return issue.getRepository();
                    });
              } catch (Exception e) {
                log.error("Repo not found for issue. url: {}, issue: {}", key, issue.getHtmlUrl());
              }
            });
    log.info("Repo fetch done");
    return repoMap;
  }

  public RepositoryDTO mapToRepositoryDTO(GHRepository ghRepository) {
    RepositoryDTO repositoryDTO =
        RepositoryDTO.builder()
            .description(ghRepository.getDescription())
            .homepage(ghRepository.getHomepage())
            .name(ghRepository.getName())
            .fullName(ghRepository.getFullName())
            .htmlUrl(
                ghRepository.getHtmlUrl() != null ? ghRepository.getHtmlUrl().toString() : null)
            .gitUrl(ghRepository.getGitTransportUrl())
            .sshUrl(ghRepository.getSshUrl())
            .cloneUrl(ghRepository.getHttpTransportUrl())
            .owner(OwnerDTO.builder().login(ghRepository.getOwnerName()).build())
            .hasIssues(ghRepository.hasIssues())
            .fork(ghRepository.isFork())
            .hasDownloads(ghRepository.hasDownloads())
            .archived(ghRepository.isArchived())
            .disabled(ghRepository.isDisabled())
            .isPrivate(ghRepository.isPrivate())
            .forksCount(ghRepository.getForksCount())
            .stargazersCount(ghRepository.getStargazersCount())
            .watchersCount(ghRepository.getWatchersCount())
            .size(ghRepository.getSize())
            .openIssuesCount(ghRepository.getOpenIssueCount())
            .subscribersCount(ghRepository.getSubscribersCount())
            .pushedAt(
                ghRepository.getPushedAt() != null ? ghRepository.getPushedAt().toString() : null)
            .build();
    try {
      repositoryDTO.setLanguages(ghRepository.listLanguages());
    } catch (IOException e) {
      repositoryDTO.setLanguages(Map.of(ghRepository.getLanguage(), -1L));
    }
    return repositoryDTO;
  }

  public Map<String, RepositoryDTO> mapToRepositoryDTO(Map<String, GHRepository> ghRepositoryList) {
    Map<String, RepositoryDTO> repoMap = new HashMap<>();

    ghRepositoryList.forEach((x, y) -> repoMap.put(x, mapToRepositoryDTO(y)));
    return repoMap;
  }

  public String getRepositoryNameFromUrl(URL url) {
    String[] path = url.getPath().split("/");
    return path[2] + "/" + path[3];
  }
}
