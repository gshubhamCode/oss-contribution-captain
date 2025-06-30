package org.fa.oss.contribution.helper.controller;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.fa.oss.contribution.helper.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summaries")
@Slf4j
public class SummaryController {

  @Autowired private SummaryService summaryService;

  @GetMapping
  public ResponseEntity<String> getCachedSummaries(
      @RequestParam(defaultValue = "0") int limit,
      @RequestParam(defaultValue = "true") boolean raw) {
    String rawJson = "{}";
    try {
      rawJson = summaryService.generateSummariesRaw(limit);
    } catch (IOException e) {
      log.error("Failed in fetching Raw summary", e);
    }
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(rawJson);
  }

  @GetMapping("/refresh")
  public IssueSummaryResultListDTO refreshSummaries(@RequestParam(defaultValue = "0") int limit) {
    return summaryService.generateSummaries(limit);
  }

  @PostMapping
  public IssueSummaryResultListDTO getSummary(@RequestBody List<IssueDTO> issueDTO) {
    return summaryService.generateSummaries(issueDTO);
  }
}
