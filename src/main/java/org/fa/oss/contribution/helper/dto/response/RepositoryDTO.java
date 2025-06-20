package org.fa.oss.contribution.helper.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDTO {

  private String description;
  private String homepage;
  private String name;
  private String fullName;
  private String htmlUrl;
  private String gitUrl;
  private String sshUrl;
  private String cloneUrl;
  private OwnerDTO owner;
  private boolean hasIssues;
  private boolean fork;
  private boolean hasDownloads;
  private boolean archived;
  private boolean disabled;

  @JsonProperty("private")
  private boolean isPrivate;

  private int forksCount;
  private int stargazersCount;
  private int watchersCount;
  private int size;
  private int openIssuesCount;
  private int subscribersCount;
  private String pushedAt;
  private Map<String, Long> languages;
}
