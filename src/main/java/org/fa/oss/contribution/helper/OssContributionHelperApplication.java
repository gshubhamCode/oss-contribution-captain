package org.fa.oss.contribution.helper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OssContributionHelperApplication {

  public static void main(String[] args) {
    SpringApplication.run(OssContributionHelperApplication.class, args);
  }
}
