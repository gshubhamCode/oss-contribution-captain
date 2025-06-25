package org.fa.oss.contribution.helper.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class OllamaRequest {
  String model;
  String prompt;
  boolean stream;
  Map<String, Object> format;
}
