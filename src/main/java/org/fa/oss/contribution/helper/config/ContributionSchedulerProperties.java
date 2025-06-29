package org.fa.oss.contribution.helper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "contribution.scheduler")
@Data
public class ContributionSchedulerProperties {

    /**
     * Enable or disable the scheduler.
     */
    private boolean enabled = true;

    /**
     * Delay in milliseconds between each run.
     */
    private long delayMs = 2 * 60 * 60 * 1000; // Default to 2 hours

    /**
     * Persist scheduler delay, to make it work after restart
    */
    private String persistFile = "config/scheduler-delay.json";

}

