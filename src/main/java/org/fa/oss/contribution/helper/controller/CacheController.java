package org.fa.oss.contribution.helper.controller;

import java.util.Map;
import org.fa.oss.contribution.helper.cache.BannersCache;
import org.fa.oss.contribution.helper.cache.IssuesCache;
import org.fa.oss.contribution.helper.cache.SummariesCache;
import org.fa.oss.contribution.helper.dto.request.CacheTtlUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

  private final IssuesCache issuesCache;
  private final SummariesCache summariesCache;
  private final BannersCache bannersCache;

  @Autowired
  public CacheController(
      IssuesCache issuesCache, SummariesCache summariesCache, BannersCache bannersCache) {
    this.issuesCache = issuesCache;
    this.summariesCache = summariesCache;
    this.bannersCache = bannersCache;
  }

  // View TTLs
  @GetMapping("/ttls")
  public Map<String, Long> getAllTtls() {
    return Map.of(
        "issuesCache", issuesCache.getMaxAgeMillis(),
        "summariesCache", summariesCache.getMaxAgeMillis(),
        "bannersCache", bannersCache.getMaxAgeMillis());
  }

  // Update TTL dynamically
  // TTL updates affect subsequent load() calls only.
  @PostMapping("/ttl")
  public ResponseEntity<String> updateTtl(@RequestBody CacheTtlUpdateRequest req) {
    switch (req.cacheName.toLowerCase()) {
      case "issuescache":
        issuesCache.setMaxAgeMillis(req.ttlMillis);
        break;
      case "summariescache":
        summariesCache.setMaxAgeMillis(req.ttlMillis);
        break;
      case "bannerscache":
        bannersCache.setMaxAgeMillis(req.ttlMillis);
        break;
      default:
        return ResponseEntity.badRequest().body("Unknown cacheName");
    }
    return ResponseEntity.ok("TTL updated for " + req.cacheName);
  }
}
