package org.fa.oss.contribution.helper.controller;

import java.io.IOException;
import java.util.List;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.service.IssuesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class IssuesController {

  @Autowired private IssuesService issuesService;

  @GetMapping("/issues")
  public List<IssueDTO> getIssues() throws IOException {
    List<IssueDTO> issues = issuesService.searchGoodFirstIssues();
    return issues;
  }
}
