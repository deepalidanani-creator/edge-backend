package com.emeritus.edge_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenantId", "topicId"})
})
public class TenantAllocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String tenantId;
    private Long topicId;
    private int allocatedSlots;

    // Required empty constructor for JPA
    public TenantAllocation() {}

    public TenantAllocation(String tenantId, Long topicId, int allocatedSlots) {
        this.tenantId = tenantId;
        this.topicId = topicId;
        this.allocatedSlots = allocatedSlots;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public int getAllocatedSlots() { return allocatedSlots; }
    public void setAllocatedSlots(int allocatedSlots) { this.allocatedSlots = allocatedSlots; }
}
