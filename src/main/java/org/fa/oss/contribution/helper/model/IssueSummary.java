package org.fa.oss.contribution.helper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.SummaryDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueSummary {
  IssueDTO issueDTO;
  SummaryDTO summary;
}
