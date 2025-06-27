package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.fa.oss.contribution.helper.model.IssueSummary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryCacheService {

  private final SummaryGenerator summaryGenerator;
  private final IssuesService issuesService;
  private final ObjectMapper objectMapper;

  private static final String SUMMARY_FILENAME = "summary.json";
  private static final int CACHE_VALIDITY_MINUTES = 120;

  public IssueSummaryResultListDTO getCachedOrGeneratedSummaries(int limit) {
    File summaryFile = new File(System.getProperty("user.dir") + File.separator + SUMMARY_FILENAME);

    if (summaryFile.exists() && !isFileOlderThan(summaryFile, CACHE_VALIDITY_MINUTES)) {
      log.info("Returning cached summaries from {}", SUMMARY_FILENAME);
      try {
        return objectMapper.readValue(summaryFile, IssueSummaryResultListDTO.class);
      } catch (IOException e) {
        log.error("Failed to read cached summary file, regenerating", e);
      }
    }
    return  generateSummariesNoCacheUse(limit);
  }

  public IssueSummaryResultListDTO generateSummariesNoCacheUse(int limit){
    try {
      List<IssueDTO> issues = issuesService.getCachedOrFetchedIssues();
      List<IssueSummary> summaries =
              summaryGenerator
                      .generateSummaries(maybeLimit(issues.stream(), limit).toList())
                      .getSummaries();

      IssueSummaryResultListDTO result =
              IssueSummaryResultListDTO.builder().summaries(summaries).count(summaries.size()).build();
      saveSummariesToCache(result);
      return result;
    } catch (Exception e) {
      log.error("Failed to generate issue summaries", e);
      return IssueSummaryResultListDTO.builder().count(0).summaries(Collections.emptyList()).build();
    }
  }

  private <T> Stream<T> maybeLimit(Stream<T> stream, int limit) {
    return limit > 0 ? stream.limit(limit) : stream;
  }

  private void saveSummariesToCache(IssueSummaryResultListDTO result) {
    summaryGenerator.saveSummaries(result);
  }

  private boolean isFileOlderThan(File file, int minutes) {
    long ageInMillis = System.currentTimeMillis() - file.lastModified();
    return ageInMillis > minutes * 60 * 1000L;
  }
}
