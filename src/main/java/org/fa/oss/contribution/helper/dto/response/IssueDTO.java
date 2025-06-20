package org.fa.oss.contribution.helper.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueDTO {
  private long id;
  private String title;
  private String url;
  private String repositoryName;
  private String description;
  private Instant updatedAt;
  private String user;
  private String userHtmlUrl;
  private String userAvatarUrl;
  private Set<String> labels;
  private String state;
  @JsonIgnore private List<String> comments;

  private RepositoryDTO repository;
}
