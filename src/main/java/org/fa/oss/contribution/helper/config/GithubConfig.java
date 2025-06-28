package org.fa.oss.contribution.helper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "github")
@Getter
@Setter
public class GithubConfig {

  public String token;
  private long issuesTtlMillis;


  public String getToken() {
    return token;
  }
}
