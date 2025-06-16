package org.fa.oss.contribution.helper.service;

import java.io.IOException;
import java.util.List;
import org.fa.oss.contribution.helper.config.GithubConfig;
import org.fa.oss.contribution.helper.constants.Github;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IssuesService {

  @Autowired GithubConfig githubConfig;

  public List<IssueDTO> searchGoodFirstIssues() throws IOException {
    GitHub github = new GitHubBuilder().withOAuthToken(githubConfig.getGithubToken()).build();
    PagedSearchIterable<GHIssue> results =
        github
            .searchIssues()
            .q("label:\"good first issue\" state:open stars:>50 is:issue")
            .sort(GHIssueSearchBuilder.Sort.UPDATED)
            .order(GHDirection.DESC)
            .list();

    List<GHIssue> issuesList = results.withPageSize(Github.PAGE_SIZE).toList();

    List<IssueDTO> issueDTOS =
        issuesList.stream()
            .map(
                issue -> {
                  String[] path = issue.getUrl().getPath().split("/");
                  return IssueDTO.builder()
                      .url(issue.getUrl().toString())
                      .title(issue.getTitle())
                      .description(issue.getTitle())
                      .repositoryName(path[2] + "/" + path[3])
                      .build();
                })
            .toList();

    return issueDTOS;
  }
}
