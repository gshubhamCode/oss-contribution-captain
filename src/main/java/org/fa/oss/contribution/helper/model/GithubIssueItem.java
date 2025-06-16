package org.fa.oss.contribution.helper.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubIssueItem {

  private String url;
  private String repository_url;
  private String comments_url;
  private String html_url;
  private long id;
  private String node_id;
  private int number;
  private String title;
  private boolean locked;
  private String state;
  private String created_at;
  private String updated_at;
  private String body;

  // Nested user, labels, assignees, milestone, etc. can be added similarly

}
