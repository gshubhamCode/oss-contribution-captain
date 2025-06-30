package org.fa.oss.contribution.helper.controller;

import org.fa.oss.contribution.helper.dto.response.SchedulerStatusDTO;
import org.fa.oss.contribution.helper.service.ContributionScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

  @Autowired private ContributionScheduler scheduler;

  @GetMapping("/status")
  public SchedulerStatusDTO getStatus() {
    return scheduler.getStatus();
  }

  @PostMapping("/set-delay")
  public ResponseEntity<String> setDelay(@RequestParam long delayMs) {
    if (delayMs < 60_000) {
      return ResponseEntity.badRequest().body("Minimum delay is 60,000 ms");
    }
    scheduler.updateDelay(delayMs);
    return ResponseEntity.ok("Updated delay to " + delayMs);
  }

  @PostMapping("/reset-delay")
  public ResponseEntity<String> resetDelay() {
    scheduler.resetDelayToDefault();
    return ResponseEntity.ok("Reset delay to default");
  }

  @PostMapping("/run-now")
  public ResponseEntity<String> runNow() {
    scheduler.runNow();
    return ResponseEntity.ok("Scheduler job triggered manually.");
  }
}
