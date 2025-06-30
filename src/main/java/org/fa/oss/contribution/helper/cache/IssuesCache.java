package org.fa.oss.contribution.helper.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.fa.oss.contribution.helper.config.CacheProperties;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IssuesCache extends JsonFileCache<List<IssueDTO>> {
  @Autowired
  public IssuesCache(ObjectMapper mapper, CacheProperties props) {
    super(mapper, props, "issues.json", new TypeReference<>() {});
  }
}
