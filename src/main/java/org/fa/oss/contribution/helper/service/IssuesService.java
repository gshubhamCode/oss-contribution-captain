package org.fa.oss.contribution.helper.service;

import org.fa.oss.contribution.helper.model.GithubIssue;
import org.fa.oss.contribution.helper.model.GithubIssueItem;
import org.fa.oss.contribution.helper.utility.GithubApiHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Service
public class IssuesService {

  private RestTemplate restTemplate = new RestTemplate();

  @Autowired
  GithubApiHelper githubApiHelper;

  public List<GithubIssueItem> getIssues() throws URISyntaxException {
    List<GithubIssueItem> issuesList = getIssueItem();
    return issuesList;
  }

  private List<GithubIssueItem> getIssueItem() throws URISyntaxException {
    List<GithubIssueItem> issuesList = new ArrayList<>();

    int pageNumber = 1;
    int itemsTillNow = 0;
    int totalCount = 0;
    do {
      String url = githubApiHelper.getIssuesUrl(pageNumber);
      ResponseEntity<GithubIssue> response = restTemplate.getForEntity(url, GithubIssue.class);
      if (response.getStatusCode() == HttpStatus.OK) {
        GithubIssue issue = response.getBody();
        totalCount = issue.getTotal_count();
        issuesList.addAll(issue.getItems());
        itemsTillNow += issue.getItems().size();
        pageNumber++;
      } else {
        throw new ErrorResponseException(response.getStatusCode());
      }
    } while (itemsTillNow < totalCount);
    return issuesList;
  }


}
