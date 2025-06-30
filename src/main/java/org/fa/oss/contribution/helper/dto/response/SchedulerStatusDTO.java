package org.fa.oss.contribution.helper.dto.response;

public record SchedulerStatusDTO(
    boolean enabled, long currentDelayMs, long defaultDelayMs, long lastRunTime) {}
