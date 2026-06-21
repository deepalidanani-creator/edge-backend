package com.emeritus.edge_backend.repository;

import com.emeritus.edge_backend.entity.TenantCohort;
import com.emeritus.edge_backend.entity.TenantCohortId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantCohortRepository extends JpaRepository<TenantCohort, TenantCohortId> {

    List<TenantCohort> findByTenantId(String tenantId);

    Optional<TenantCohort> findByTenantIdAndCohortName(String tenantId, String cohortName);
}
