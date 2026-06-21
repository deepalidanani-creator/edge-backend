package com.emeritus.edge_backend.repository;

import com.emeritus.edge_backend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByIdAndTenantId(String id, String tenantId);

    List<Employee> findByTenantId(String tenantId);
}
