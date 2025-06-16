package org.fa.oss.contribution.helper.controller;

import org.fa.oss.contribution.helper.model.GithubIssueItem;
import org.fa.oss.contribution.helper.service.IssuesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/")
public class IssuesController {

  @Autowired private IssuesService issuesService;

  @GetMapping("/issues")
  public List<GithubIssueItem> getIssues() throws URISyntaxException {
    List<GithubIssueItem> issues = issuesService.getIssues();
    return issues;
  }
}
