package org.fa.oss.contribution.helper.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueSummary {
  IssueDTO issueDTO;
  String summary;
  List<String> languages;
}
