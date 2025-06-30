package org.fa.oss.contribution.helper.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.fa.oss.contribution.helper.config.CacheProperties;
import org.fa.oss.contribution.helper.notification.model.NotificationBanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BannersCache extends JsonFileCache<List<NotificationBanner>> {
  @Autowired
  public BannersCache(ObjectMapper mapper, CacheProperties props) {
    super(mapper, props, "banners.json", new TypeReference<List<NotificationBanner>>() {});
  }
}
