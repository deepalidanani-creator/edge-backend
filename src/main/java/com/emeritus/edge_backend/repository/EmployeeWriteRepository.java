package com.emeritus.edge_backend.repository;

import com.emeritus.edge_backend.entity.Employee;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Write operations for {@link Employee} and cohort membership.
 * Used when applying reconciler preview operations (not used in preview-only flow today).
 */
@Repository
public class EmployeeWriteRepository {

	private final EmployeeRepository employeeRepository;

	public EmployeeWriteRepository(EmployeeRepository employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	@Transactional
	public Employee createEmployee(String tenantId, String employeeId, String name, String email, String role) {
		Employee employee = new Employee();
		employee.setId(employeeId);
		employee.setTenantId(tenantId);
		employee.setName(name);
		employee.setEmailid(email);
		employee.setRole(role);
		employee.setCohortIds(new ArrayList<>());
		return employeeRepository.save(employee);
	}

	@Transactional
	public Employee createEmployeeWithCohortMembership(String tenantId, String employeeId, String name, String email,
			String role, List<String> cohortIds) {
		Employee employee = new Employee();
		employee.setId(employeeId);
		employee.setTenantId(tenantId);
		employee.setName(name);
		employee.setEmailid(email);
		employee.setRole(role);
		employee.setCohortIds(new ArrayList<>(distinctNonBlank(cohortIds)));
		return employeeRepository.save(employee);
	}

	@Transactional
	public Employee updateEmployeeDetails(String tenantId, String employeeId, String name, String role) {
		Employee employee = requireEmployee(tenantId, employeeId);
		employee.setName(name);
		employee.setRole(role);
		return employeeRepository.save(employee);
	}

	@Transactional
	public Employee updateEmployeeDetailsWithCohortMembership(String tenantId, String employeeId, String name,
			String role, List<String> cohortIdsToAdd, List<String> cohortIdsToRemove) {
		Employee employee = requireEmployee(tenantId, employeeId);
		employee.setName(name);
		employee.setRole(role);
		applyCohortChanges(employee, cohortIdsToAdd, cohortIdsToRemove);
		return employeeRepository.save(employee);
	}

	private Employee requireEmployee(String tenantId, String employeeId) {
		return employeeRepository.findByIdAndTenantId(employeeId, tenantId)
				.orElseThrow(() -> new IllegalArgumentException(
						"Employee not found for tenant: " + employeeId + " / " + tenantId));
	}

	private static void applyCohortChanges(Employee employee, List<String> cohortIdsToAdd,
			List<String> cohortIdsToRemove) {
		Set<String> membership = new LinkedHashSet<>(employee.getCohortIds());
		for (String cohortId : distinctNonBlank(cohortIdsToRemove)) {
			membership.remove(cohortId);
		}
		for (String cohortId : distinctNonBlank(cohortIdsToAdd)) {
			membership.add(cohortId);
		}
		employee.setCohortIds(new ArrayList<>(membership));
	}

	private static List<String> distinctNonBlank(List<String> cohortIds) {
		if (cohortIds == null || cohortIds.isEmpty()) {
			return List.of();
		}
		return cohortIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
	}
}
