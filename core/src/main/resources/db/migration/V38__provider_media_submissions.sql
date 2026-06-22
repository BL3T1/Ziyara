-- V38: Provider media submissions — pending admin approval before going live
CREATE TABLE IF NOT EXISTS provider_media_submissions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id      UUID         NOT NULL,
    service_id       UUID,
    image_type       VARCHAR(64)  NOT NULL,
    context_key      VARCHAR(128),
    file_url         VARCHAR(1000) NOT NULL,
    alt_text         VARCHAR(255),
    is_primary       BOOLEAN      NOT NULL DEFAULT false,
    status           VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    submitted_by     UUID         NOT NULL,
    submitted_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    reviewed_by      UUID,
    reviewed_at      TIMESTAMP,
    review_note      VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_media_sub_provider ON provider_media_submissions(provider_id);
CREATE INDEX IF NOT EXISTS idx_media_sub_status   ON provider_media_submissions(status);
CREATE INDEX IF NOT EXISTS idx_media_sub_service  ON provider_media_submissions(service_id);
