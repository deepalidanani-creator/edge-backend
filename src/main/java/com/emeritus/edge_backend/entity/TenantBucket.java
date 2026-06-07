package com.emeritus.edge_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TenantBucket {
    
    @Id
    private String tenantId; // e.g., "vantage-fi"
    private String bucketType; // BUCKET_01, BUCKET_02, BUCKET_03
    private int maxLimit; // 1, 3, or 8 sessions maximum global capacity

    // Required by JPA
    public TenantBucket() {}

    public TenantBucket(String tenantId, String bucketType, int maxLimit) {
        this.tenantId = tenantId;
        this.bucketType = bucketType;
        this.maxLimit = maxLimit;
    }

    // Getters and Setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getBucketType() { return bucketType; }
    public void setBucketType(String bucketType) { this.bucketType = bucketType; }
    public int getMaxLimit() { return maxLimit; }
    public void setMaxLimit(int maxLimit) { this.maxLimit = maxLimit; }
}
