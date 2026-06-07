package com.emeritus.edge_backend.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    private String id;

    private String tenantId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_cohorts", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "cohort_id")
    private List<String> cohortIds = new ArrayList<>();

    public Employee() {}

    public Employee(String id, String tenantId, List<String> cohortIds) {
        this.id = id;
        this.tenantId = tenantId;
        this.cohortIds = cohortIds;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public List<String> getCohortIds() { return cohortIds; }
    public void setCohortIds(List<String> cohortIds) { this.cohortIds = cohortIds; }
}
