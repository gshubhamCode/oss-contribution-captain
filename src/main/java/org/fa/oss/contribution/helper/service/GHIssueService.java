package org.fa.oss.contribution.helper.service;

import java.io.IOException;
import java.util.List;
import org.fa.oss.contribution.helper.config.GithubConfig;
import org.fa.oss.contribution.helper.constants.Github;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GHIssueService {

  @Autowired GithubConfig githubConfig;

  private List<GHIssue> issues;

  List<GHIssue> getGHIssues() throws IOException {
    if (issues == null) {
      GitHub github = new GitHubBuilder().withOAuthToken(githubConfig.getGithubToken()).build();

      PagedSearchIterable<GHIssue> results =
          github
              .searchIssues()
              .q("label:\"good first issue\" state:open is:issue")
              .sort(GHIssueSearchBuilder.Sort.UPDATED)
              .order(GHDirection.DESC)
              .list();

      issues = results.withPageSize(Github.PAGE_SIZE).toList();
    }
    return issues;
  }
}
