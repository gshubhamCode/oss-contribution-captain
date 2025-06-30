package org.fa.oss.contribution.helper.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Getter
@Slf4j
public class CentralCacheService {
  public final IssuesCache issueCache;
  public final SummariesCache summaryCache;
  public final BannersCache bannerCache;

  @Autowired
  public CentralCacheService(
      IssuesCache issueCache, SummariesCache summaryCache, BannersCache bannerCache) {
    this.issueCache = issueCache;
    this.summaryCache = summaryCache;
    this.bannerCache = bannerCache;
  }
}
