package org.fa.oss.contribution.helper.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import org.springframework.core.annotation.AliasFor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubIssue {

  private int total_count;
  private boolean incomplete_results;

  @JsonAlias("items")
  private List<GithubIssueItem> items;
}
