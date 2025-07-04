package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.config.ContributionSchedulerProperties;
import org.fa.oss.contribution.helper.config.GithubConfig;
import org.fa.oss.contribution.helper.config.RunPodConfig;
import org.fa.oss.contribution.helper.dto.response.SchedulerStatusDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContributionScheduler {

  private SummaryService summaryService;
  private IssuesService issueService;
  private RunPodManager runPodManager;

  private RunPodConfig runPodConfig;


  @Autowired
  private GithubConfig githubConfig;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private final ContributionSchedulerProperties contributionSchedulerProperties;

  private ScheduledExecutorService executor;
  private ScheduledFuture<?> future;
  private long currentDelayMs;
  private long lastRunTime = -1;

  @Autowired private ObjectMapper objectMapper;

  @Autowired
  public ContributionScheduler(
      IssuesService issueService,
      SummaryService summaryService,
      ContributionSchedulerProperties properties,
      RunPodManager runPodManager,
      RunPodConfig runPodConfig) {
    this.summaryService = summaryService;
    this.issueService = issueService;
    this.contributionSchedulerProperties = properties;
    this.runPodManager = runPodManager;
    this.runPodConfig = runPodConfig;
    this.currentDelayMs = properties.getDelayMs();
  }

  private record DelayWrapper(long delayMs) {}

  @PostConstruct
  public void startScheduler() {
    if (!contributionSchedulerProperties.isEnabled()) {
      log.info("Scheduler disabled via config.");
      return;
    }

    executor = Executors.newSingleThreadScheduledExecutor();

    // Load persisted delay if available
    File persistFile = new File(contributionSchedulerProperties.getPersistFile());
    if (persistFile.exists()) {
      try {
        currentDelayMs = objectMapper.readTree(persistFile).get("delayMs").asLong();
        log.info("Loaded persisted delay: {} ms", currentDelayMs);
      } catch (IOException e) {
        log.warn("Failed to load persisted delay, using default.", e);
        currentDelayMs = contributionSchedulerProperties.getDelayMs();
      }
    } else {
      currentDelayMs = contributionSchedulerProperties.getDelayMs();
    }

    scheduleWithDelay(currentDelayMs);
  }

  public void runNow() {
    if (!isRunning.compareAndSet(false, true)) {
      log.warn("Manual trigger skipped: job is already running.");
      return;
    }

    executor.execute(this::runJob); // run immediately in background
  }

  private void scheduleWithDelay(long delayMs) {
    if (future != null && !future.isCancelled()) {
      future.cancel(false);
    }
    future = executor.scheduleWithFixedDelay(this::runJob, 0, delayMs, TimeUnit.MILLISECONDS);
    this.currentDelayMs = delayMs;
    persistDelay();
    log.info("Scheduler scheduled with delay {} ms", delayMs);
  }

  private void persistDelay() {
    File file = new File(contributionSchedulerProperties.getPersistFile());
    try {
      file.getParentFile().mkdirs();
      objectMapper.writeValue(file, new DelayWrapper(currentDelayMs));
    } catch (IOException e) {
      log.error("Failed to persist delay", e);
    }
  }

  public void updateDelay(long newDelayMs) {
    log.info("Updating scheduler delay to {} ms", newDelayMs);
    scheduleWithDelay(newDelayMs);
  }

  public void resetDelayToDefault() {
    log.info(
        "Resetting scheduler delay to default ({} ms)",
        contributionSchedulerProperties.getDelayMs());
    scheduleWithDelay(contributionSchedulerProperties.getDelayMs());
  }

  @PreDestroy
  public void shutdown() {
    if (executor != null) executor.shutdownNow();
  }

  private void runJob() {
    GitHubUploader gitHubUploader = null;
    String targetPath = generateLogTargetPath();
    String commitMessage = "Scheduled job logs upload " + targetPath;

    log.info(">>> ENTERED runJob(), isRunning: {}", isRunning.get());
    if (!isRunning.compareAndSet(false, true)) {
      log.warn("Scheduler already running.");
      return;
    }

    try {
      log.info("Running scheduled job: generateIssues → generateSummary");
      log.info("Starting issue generation...");
      issueService.generateIssues();
      log.info("Issue generation complete.");

      log.info("Starting scheduled summary generation...");
      summaryService.generateSummaries(0);
      log.info("Summary generation complete.");

      // Upload logs before deleting pod
      try {
        log.info("Uploading logs before deleting pod...");
        gitHubUploader = new GitHubUploader( githubConfig.getToken(), "gshubhamCode/oss-captain-api-logs");
        gitHubUploader.uploadFolderInZip("./logs", targetPath, commitMessage);
        log.info("Logs upload complete.");
      } catch (Exception e) {
        log.error("Error uploading logs before pod deletion", e);
      }

      runPodManager.deletePod(runPodConfig.getSelfPodId());

    } catch (Exception e) {
      log.error("Error during scheduled task", e);
      // Upload logs before deleting pod
      try {
        log.info("Uploading logs before deleting pod...");
        gitHubUploader.uploadFolderInZip("./logs", targetPath, commitMessage);
        log.info("Logs upload complete.");
      } catch (Exception ex) {
        log.error("Error uploading logs before pod deletion", ex);
      }
    } finally {
      log.info("Reset Scheduler status  isRunning: {}", isRunning.get());
      isRunning.set(false);
      log.info("Reset done  isRunning: {}", isRunning.get());
    }
  }

  public String generateLogTargetPath() {
    LocalDateTime now = LocalDateTime.now();

    String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String time = now.format(DateTimeFormatter.ofPattern("HHmmss"));

    return String.format("logs/%s/logs_%s_%s.zip", date, date, time);
  }

  public SchedulerStatusDTO getStatus() {
    return new SchedulerStatusDTO(
        contributionSchedulerProperties.isEnabled(),
        currentDelayMs,
        contributionSchedulerProperties.getDelayMs(),
        lastRunTime);
  }
}
