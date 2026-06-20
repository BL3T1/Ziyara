package com.ziyara.backend.application.exception;

/** Thrown when a single dashboard KPI metric fails to load. Caught and suppressed to return zero/default. */
public class DashboardMetricException extends RuntimeException {

    private final String metricName;

    public DashboardMetricException(String metricName, Throwable cause) {
        super("Dashboard metric '" + metricName + "' failed: " + cause.getMessage(), cause);
        this.metricName = metricName;
    }

    public String getMetricName() {
        return metricName;
    }
}
