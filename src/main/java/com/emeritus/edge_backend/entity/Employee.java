package com.emeritus.edge_backend.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employee", uniqueConstraints= {@UniqueConstraint(columnNames= {"email_id"})})
public class Employee {

    @Id
    private String id;

    private String tenantId;
    
    @Column(name = "name")
    private String name;
    @Column(name = "email_id")
	private String emailid;
    @Column(name = "role")
    private String role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_cohorts", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "cohort_id")
    private List<String> cohortIds = new ArrayList<>();

    public Employee() {}

    public Employee(String id, String tenantId, List<String> cohortIds, String name, String emailId, String role) {
        this.id = id;
        this.tenantId = tenantId;
        this.cohortIds = cohortIds;
        this.name= name;
        this.emailid = emailId;
        this.role = role;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public List<String> getCohortIds() { return cohortIds; }
    public void setCohortIds(List<String> cohortIds) { this.cohortIds = cohortIds; }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmailid() {
		return emailid;
	}

	public void setEmailid(String emailid) {
		this.emailid = emailid;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
    
    
}
