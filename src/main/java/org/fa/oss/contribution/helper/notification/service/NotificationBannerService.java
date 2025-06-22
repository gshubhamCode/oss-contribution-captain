package org.fa.oss.contribution.helper.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.core.type.TypeReference;
import org.fa.oss.contribution.helper.notification.model.NotificationBanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationBannerService {

    private final Path bannerFile = Paths.get("cache", "banners.json");
  private final List<NotificationBanner> banners = new CopyOnWriteArrayList<>();

  @Autowired
    ObjectMapper objectMapper;

    @PostConstruct
    public void loadBanners() {
        try {
            if (Files.exists(bannerFile)) {
                List<NotificationBanner> loaded = new ObjectMapper().readValue(
                        bannerFile.toFile(),
                        new TypeReference<>() {}
                );
                banners.clear();
                banners.addAll(loaded);
            }
        } catch (Exception e) {
            // log
        }
        cleanupExpired();  // remove expired banners on boot
    }

    public List<NotificationBanner> getBanners() {
        cleanupExpired();
        return banners;
    }

    public void addBanner(NotificationBanner banner) {
        banner.setId(UUID.randomUUID().toString());
        banner.setTimestamp(System.currentTimeMillis());

        if (banner.getExpiryTime() == 0) {
            banner.setExpiryTime(System.currentTimeMillis() + (60 * 60 * 1000)); // default 1 hour
        }

        banners.add(banner);
        save();
    }

    public boolean removeBanner(String id) {
        boolean removed = banners.removeIf(b -> b.getId().equals(id));
        if (removed) save();
        return removed;
    }

    public boolean extendExpiry(String id, long additionalMillis) {
        Optional<NotificationBanner> bannerOpt = banners.stream()
                .filter(b -> b.getId().equals(id))
                .findFirst();

        if (bannerOpt.isPresent()) {
            NotificationBanner banner = bannerOpt.get();
            banner.setExpiryTime(banner.getExpiryTime() + additionalMillis);
            save();
            return true;
        }
        return false;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        boolean removed = banners.removeIf(b -> b.getExpiryTime() < now);
        if (removed) save();
    }

    private void save() {
        try {
            Files.createDirectories(bannerFile.getParent());
            objectMapper.writeValue(bannerFile.toFile(), banners);
        } catch (IOException e) {
            // log
        }
    }
}

