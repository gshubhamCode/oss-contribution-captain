package org.fa.oss.contribution.helper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "runpod")
@Getter
@Setter
public class RunPodConfig {

  private String apiKey;
  private String vllmApiKey;
  private String vllmPodId;
}
