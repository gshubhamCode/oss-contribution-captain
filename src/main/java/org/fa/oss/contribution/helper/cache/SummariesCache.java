package org.fa.oss.contribution.helper.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fa.oss.contribution.helper.config.CacheProperties;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SummariesCache extends JsonFileCache<IssueSummaryResultListDTO> {
  @Autowired
  public SummariesCache(ObjectMapper mapper, CacheProperties props) {
    super(mapper, props, "summaries.json", new TypeReference<IssueSummaryResultListDTO>() {});
  }
}
