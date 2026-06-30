package com.ziyara.backend.domain.entity;

/** Projection joining a failed delivery with its subscription's url and secret for retry. */
public record WebhookRetryTask(WebhookDelivery delivery, String url, String secret) {}
