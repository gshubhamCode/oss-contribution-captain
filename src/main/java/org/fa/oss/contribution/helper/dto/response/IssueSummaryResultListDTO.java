package org.fa.oss.contribution.helper.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fa.oss.contribution.helper.model.IssueSummary;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueSummaryResultListDTO {
  long count;
  List<IssueSummary> summaries;
}
