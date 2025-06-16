package org.fa.oss.contribution.helper.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueDTO {
  private String title;
  private String url;
  private String repositoryName;
  private String description;
}
