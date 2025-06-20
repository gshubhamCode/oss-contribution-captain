package org.fa.oss.contribution.helper.dto.response;

import java.time.ZonedDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OllamaResponse {
  private String model;
  private ZonedDateTime createdAt;
  private String response;
  private boolean done;
  private String doneReason;
  @JsonIgnore
  private List<Integer> context;
  private long totalDuration;
  private long loadDuration;
  private int promptEvalCount;
  private long promptEvalDuration;
  private int evalCount;
  private long evalDuration;
}
