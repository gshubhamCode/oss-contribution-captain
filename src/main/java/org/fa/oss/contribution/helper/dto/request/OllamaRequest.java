package org.fa.oss.contribution.helper.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OllamaRequest {
  String model;
  String prompt;
  boolean stream;
  String format;
}
