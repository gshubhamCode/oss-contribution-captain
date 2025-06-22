package org.fa.oss.contribution.helper.notification;

import org.fa.oss.contribution.helper.notification.model.NotificationBanner;
import org.fa.oss.contribution.helper.notification.service.NotificationBannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/banners")
public class NotificationController {

  @Autowired private NotificationBannerService bannerService;

  @GetMapping
  public ResponseEntity<List<NotificationBanner>> getAllBanners() {
    return ResponseEntity.ok(bannerService.getBanners());
  }

  @PostMapping
  public ResponseEntity<?> addBanner(@RequestBody NotificationBanner banner) {
    bannerService.addBanner(banner);
    return ResponseEntity.ok("Banner added");
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteBanner(@PathVariable String id) {
    boolean removed = bannerService.removeBanner(id);
    return removed ? ResponseEntity.ok("Deleted") : ResponseEntity.notFound().build();
  }

  @PostMapping("/{id}/extend")
  public ResponseEntity<?> extendBannerExpiry(
      @PathVariable String id, @RequestParam long additionalMillis) {
    boolean extended = bannerService.extendExpiry(id, additionalMillis);
    return extended ? ResponseEntity.ok("Extended") : ResponseEntity.notFound().build();
  }
}
