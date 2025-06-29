package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.config.RunPodConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;


@Slf4j
@Service
public class RunPodManager {

  private final WebClient webClient;
  private final RunPodConfig runPodConfig;

  public RunPodManager(RunPodConfig runPodConfig) {
    this.runPodConfig = runPodConfig;
    this.webClient =
        WebClient.builder()
            .baseUrl("https://rest.runpod.io")
            .defaultHeader("Authorization", "Bearer " + runPodConfig.getApiKey())
            .build();
  }

  public boolean startPod() {
    try {
      HttpStatusCode status =
          webClient
              .post()
              .uri("/v1/pods/" + runPodConfig.getPodId() + "/start")
              .retrieve()
              .toBodilessEntity()
              .map(response -> response.getStatusCode())
              .block();

      if (status != HttpStatus.OK && status != HttpStatus.ACCEPTED) {
        throw new RuntimeException(
            "Failed to start pod: " + runPodConfig.getPodId() + ", status: " + status);
      }

      log.info(
          "Start request accepted for pod " + runPodConfig.getPodId() + " with status: " + status);
      return true;
    } catch (WebClientResponseException e) {
      log.error("Error starting pod: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
      throw e;
    }
  }

  public String waitForRunningPod() throws InterruptedException {
    String status;
    String ip = null;
    int maxAttempts = 40;

    for (int i = 0; i < maxAttempts; i++) { // up to ~4 minutes
      JsonNode pod =
          webClient
              .get()
              .uri("/v1/pods/" + runPodConfig.getPodId())
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block();

      status = pod.get("desiredStatus").asText();
      ip = pod.get("publicIp").asText();
      if ("RUNNING".equals(status) && ip!= null && !ip.trim().equals("")) {
        ip = pod.get("publicIp").asText();
        break;
      }
      log.warn(
          "Attempt {}: Pod status (status={}). Pod public ip not available yet. Waiting 1 minute before retry.",
          i + 1, status);
      Thread.sleep(60_000);
    }

    if (ip == null) throw new RuntimeException("Pod never became ready after 40 minutes");
    log.info("Public ip of pod {}:{}", runPodConfig.getPodId(), ip );
    log.info("Waiting for 3 minutes to let LLM initialise on pod");
    Thread.sleep(180_000);
    return ip;
  }

  public boolean stopPod() {
    HttpStatusCode status =
        webClient
            .post()
            .uri("/v1/pods/" + runPodConfig.getPodId() + "/stop")
            .retrieve()
            .toBodilessEntity()
            .map(response -> response.getStatusCode())
            .block();

    if (status != HttpStatus.OK) {
      throw new RuntimeException(
          "Failed to stop pod: " + runPodConfig.getPodId() + ", status: " + status);
    }

    log.info(
        "Successfully sent stop request for pod: "
            + runPodConfig.getPodId()
            + " with status: "
            + status);
    return true;
  }
}
