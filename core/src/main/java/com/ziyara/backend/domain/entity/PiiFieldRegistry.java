package com.ziyara.backend.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class PiiFieldRegistry {

    private UUID id;
    private String tableName;
    private String columnName;
    private String piiCategory;
    private Boolean encryptionRequired;
    private String gdprArticle;
    private LocalDateTime lastReviewedAt;

    public PiiFieldRegistry() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getPiiCategory() { return piiCategory; }
    public void setPiiCategory(String piiCategory) { this.piiCategory = piiCategory; }

    public Boolean getEncryptionRequired() { return encryptionRequired; }
    public void setEncryptionRequired(Boolean encryptionRequired) { this.encryptionRequired = encryptionRequired; }

    public String getGdprArticle() { return gdprArticle; }
    public void setGdprArticle(String gdprArticle) { this.gdprArticle = gdprArticle; }

    public LocalDateTime getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(LocalDateTime lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
}
