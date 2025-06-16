package org.fa.oss.contribution.helper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GithubConfig {

  @Value("${github.token}")
  public String githubToken;

  public String getGithubToken() {
    return githubToken;
  }
}
