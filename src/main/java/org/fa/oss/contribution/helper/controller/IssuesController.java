package org.fa.oss.contribution.helper.controller;

import java.util.List;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.service.IssuesService;
import org.fa.oss.contribution.helper.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IssuesController {

  @Autowired private IssuesService issuesService;

  @Autowired private SummaryService summaryService;

  @GetMapping("/issues")
  public List<IssueDTO> getIssues()  {
    List<IssueDTO> issues = issuesService.getIssues();
    return issues.stream().toList();
  }

}
