package org.fa.oss.contribution.helper.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDTO {
  private String main;
  private String validationOrRequirement;
  private String attemptedFixes;
  private String otherNotes;
  private String summaryText;
  private boolean validJson;
}
