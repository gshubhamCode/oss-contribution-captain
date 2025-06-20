package org.fa.oss.contribution.helper.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDTO {
  private String login;
  private String avatarUrl;
  private String htmlUrl;
}
