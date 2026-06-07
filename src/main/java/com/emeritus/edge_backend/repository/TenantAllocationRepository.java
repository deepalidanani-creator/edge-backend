package com.emeritus.edge_backend.repository;

import com.emeritus.edge_backend.entity.TenantAllocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantAllocationRepository extends JpaRepository<TenantAllocation, Long> {

    List<TenantAllocation> findByTenantId(String tenantId);

    // CRITICAL: This lock prevents concurrent over-booking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TenantAllocation> findByTenantIdAndTopicId(String tenantId, Long topicId);
}
