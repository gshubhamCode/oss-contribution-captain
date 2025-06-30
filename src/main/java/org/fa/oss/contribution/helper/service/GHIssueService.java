package org.fa.oss.contribution.helper.service;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.config.GithubConfig;
import org.fa.oss.contribution.helper.constants.Github;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GHIssueService {

  @Autowired private GithubConfig githubConfig;

  private List<GHIssue> issues;
  private long lastFetchTime = 0L;

  public List<GHIssue> getGHIssues() throws IOException {
    log.info("Fetching GHIssues");
    long now = System.currentTimeMillis();
    long ttl = githubConfig.getIssuesTtlMillis();

    if (issues == null || (now - lastFetchTime) > ttl) {
      return forceRefreshIssues(); // auto-refresh if stale
    }

    return issues;
  }

  public List<GHIssue> forceRefreshIssues() throws IOException {
    log.info("Fetching issues using force refresh");
    GitHub github = new GitHubBuilder().withOAuthToken(githubConfig.getToken()).build();

    PagedSearchIterable<GHIssue> results =
        github
            .searchIssues()
            .q("label:\"good first issue\" state:open is:issue")
            .sort(GHIssueSearchBuilder.Sort.UPDATED)
            .order(GHDirection.DESC)
            .list();

    issues = results.withPageSize(Github.PAGE_SIZE).toList();
    lastFetchTime = System.currentTimeMillis();

    return issues;
  }
}
