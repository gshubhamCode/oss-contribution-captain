package org.fa.oss.contribution.helper.controller;

import org.fa.oss.contribution.helper.service.IssuesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class Greet {

  @GetMapping("greet")
  public String greet() {
    return "Welcome Fellow OSS Contributor";
  }
}
