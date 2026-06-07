package com.emeritus.edge_backend.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
// CRITICAL: Enforces that a tenant cannot reuse the same idempotency key twice
@Table(name = "masterclass_session",
    indexes = {
        @Index(name = "idx_session_tenant_date", columnList = "tenantId, sessionDate")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenantId", "idempotencyKey"})
    })
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tenantId;
    private Long topicId;
    private Long speakerId;
    private LocalDate sessionDate;
    private String title;
    private String theme;
    private boolean audienceAll;

    // Stores targeted cohort strings in a joined collection table automatically
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_cohorts", joinColumns = @JoinColumn(name = "session_id"))
    private List<String> cohortIds = new ArrayList<>();

    private String idempotencyKey;

    public Session() {}

    // Getters and Setters
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Long getSpeakerId() { return speakerId; }
    public void setSpeakerId(Long speakerId) { this.speakerId = speakerId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public boolean isAudienceAll() { return audienceAll; }
    public void setAudienceAll(boolean audienceAll) { this.audienceAll = audienceAll; }
    public List<String> getCohortIds() { return cohortIds; }
    public void setCohortIds(List<String> cohortIds) { this.cohortIds = cohortIds; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
