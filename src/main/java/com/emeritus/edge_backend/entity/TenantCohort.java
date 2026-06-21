package com.emeritus.edge_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "tenant_cohort",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "cohort_name"})
)
@IdClass(TenantCohortId.class)
public class TenantCohort {

    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    @Id
    @Column(name = "cohort_id")
    private String cohortId;

    @Column(name = "cohort_name")
    private String cohortName;

    protected TenantCohort() {}

    public TenantCohort(String cohortId, String cohortName, String tenantId) {
        this.tenantId = tenantId;
        this.cohortId = cohortId;
        this.cohortName = cohortName;
    }

    public String getCohortId() {
        return cohortId;
    }

    public void setCohortId(String cohortId) {
        this.cohortId = cohortId;
    }

    public String getCohortName() {
        return cohortName;
    }

    public void setCohortName(String cohortName) {
        this.cohortName = cohortName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
