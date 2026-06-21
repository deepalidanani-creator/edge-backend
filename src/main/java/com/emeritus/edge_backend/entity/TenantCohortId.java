package com.emeritus.edge_backend.entity;

import java.io.Serializable;
import java.util.Objects;

public class TenantCohortId implements Serializable {

    private static final long serialVersionUID = 1L;
	private String tenantId;
    private String cohortId;

    public TenantCohortId() {}

    public TenantCohortId(String tenantId, String cohortId) {
        this.tenantId = tenantId;
        this.cohortId = cohortId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TenantCohortId that = (TenantCohortId) o;
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(cohortId, that.cohortId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, cohortId);
    }
}
