package org.fa.oss.contribution.helper.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBanner {
    private String id;
    private String message;
    private long timestamp;   // Time it was added
    private long expiryTime;  // Time after which it should disappear

    // Getters/setters/constructors
}
