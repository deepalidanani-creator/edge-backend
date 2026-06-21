package com.emeritus.edge_backend;

import com.emeritus.edge_backend.dto.request.CSVEmployeeCohortDto;
import com.emeritus.edge_backend.dto.response.CSVEmployeeCohortResponse;
import com.emeritus.edge_backend.dto.response.CsvEmployeeCohortRowResult;
import com.emeritus.edge_backend.dto.response.ReconciliationOperation;
import com.emeritus.edge_backend.dto.response.RowCategory;
import com.emeritus.edge_backend.entity.Employee;
import com.emeritus.edge_backend.entity.TenantCohort;
import com.emeritus.edge_backend.repository.EmployeeRepository;
import com.emeritus.edge_backend.repository.TenantCohortRepository;
import com.emeritus.edge_backend.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FileUploadService#reconcileRows(List, String)}.
 *
 * <p>Scenario index — see class-level summary in project docs / PR description:
 * validation, create, update, cohort diff, conflict, and regression cases for bugs 1–10.
 */
@ExtendWith(MockitoExtension.class)
class FileUploadServiceReconciliationTests {

	private static final String TENANT = "vantage-fi";

	@Mock
	private EmployeeRepository employeeRepository;

	@Mock
	private TenantCohortRepository tenantCohortRepository;

	@InjectMocks
	private FileUploadService fileUploadService;

	@BeforeEach
	void seedTenantCohortCatalog() {
		when(tenantCohortRepository.findByTenantId(TENANT)).thenReturn(List.of(
				cohort("leadership-2026", "Senior Leadership"),
				cohort("ai-capability-build", "AI Capability Build"),
				cohort("managers-q2", "Managers Q2"),
				cohort("engineering", "Engineering")));
	}

	@Test
	void responseValidWhenAllRowsValid() {
		when(employeeRepository.findAllById(anyIterable())).thenReturn(List.of());

		CSVEmployeeCohortResponse response = reconcile(csvRow("emp-003", "new@vantage.com", "New User", null, null));

		assertThat(response.isValid()).isTrue();
		assertThat(row(response, 2).isValid()).isTrue();
	}

	@Test
	void responseInvalidWhenAnyRowInvalid() {
		when(employeeRepository.findAllById(anyIterable())).thenReturn(List.of());

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "a@vantage.com", "A", null, null),
				csvRow("emp-001", "b@vantage.com", "B", null, null));

		assertThat(response.isValid()).isFalse();
	}

	@Test
	void duplicateEmployeeIdMarksBothRowsInvalid() {
		when(employeeRepository.findAllById(anyIterable())).thenReturn(List.of());

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "a@vantage.com", "A", null, null),
				csvRow("emp-001", "b@vantage.com", "B", null, null));

		assertThat(row(response, 2).isValid()).isFalse();
		assertThat(row(response, 3).isValid()).isFalse();
		assertThat(row(response, 2).getRowCategory()).isEqualTo(RowCategory.CORRECTION);
		assertThat(row(response, 2).getErrors()).extracting("code").contains("DUPLICATE_EMPLOYEE");
	}

	@Test
	void duplicateEmailMarksBothRowsInvalid() {
		when(employeeRepository.findAllById(anyIterable())).thenReturn(List.of());

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "same@vantage.com", "A", null, null),
				csvRow("emp-002", "same@vantage.com", "B", null, null));

		assertThat(response.isValid()).isFalse();
		assertThat(row(response, 2).getErrors()).extracting("code").contains("DUPLICATE_EMAIL");
		assertThat(row(response, 3).getErrors()).extracting("code").contains("DUPLICATE_EMAIL");
	}

	@Test
	void missingRequiredFieldIsCorrection() {
		CSVEmployeeCohortDto row = csvRow("", "a@vantage.com", "A", null, null);
		CSVEmployeeCohortResponse response = reconcile(row);

		assertThat(response.isValid()).isFalse();
		assertThat(row(response, 2).getRowCategory()).isEqualTo(RowCategory.CORRECTION);
		assertThat(row(response, 2).getErrors()).extracting("code").contains("REQUIRED_FIELD_MISSING");
	}

	@Test
	void createNewEmployeeWithoutCohorts() {
		when(employeeRepository.findAllById(Set.of("emp-003"))).thenReturn(List.of());

		CSVEmployeeCohortResponse response = reconcile(csvRow("emp-003", "new@vantage.com", "New User", null, null));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.getRowCategory()).isEqualTo(RowCategory.CREATE);
		assertThat(rowResult.getOperations()).containsExactly(ReconciliationOperation.CREATE_EMPLOYEE);
	}

	@Test
	void createNewEmployeeWithValidCohorts() {
		when(employeeRepository.findAllById(Set.of("emp-003"))).thenReturn(List.of());

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-003", "new@vantage.com", "New User", null, "Senior Leadership;AI Capability Build"));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.getOperations()).containsExactly(ReconciliationOperation.CREATE_EMPLOYEE,
				ReconciliationOperation.ADD_COHORT_MEMBERSHIP);
	}

	@Test
	void unknownCohortOnCreate() {
		when(employeeRepository.findAllById(Set.of("emp-003"))).thenReturn(List.of());

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-003", "new@vantage.com", "New User", null, "Does Not Exist"));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.isValid()).isFalse();
		assertThat(rowResult.getErrors()).extracting("code").contains("UNKNOWN_COHORT");
	}

	@Test
	void existingEmployeeNoChangesNoOp() {
		Employee existing = employee("emp-001", "Jane Doe", "jane@vantage.com", "EMPLOYEE",
				List.of("leadership-2026", "managers-q2"));
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of(existing));

		CSVEmployeeCohortResponse response = reconcile(csvRow("emp-001", "jane@vantage.com", "Jane Doe", "EMPLOYEE",
				"Senior Leadership;Managers Q2"));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.getRowCategory()).isEqualTo(RowCategory.SKIP_OP);
		assertThat(rowResult.getOperations()).containsExactly(ReconciliationOperation.NO_OP);
	}

	@Test
	void existingEmployeeAddCohortPreservesOperationWhenDetailsUnchanged() {
		Employee existing = employee("emp-001", "Jane Doe", "jane@vantage.com", "EMPLOYEE", List.of("leadership-2026"));
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of(existing));

		CSVEmployeeCohortResponse response = reconcile(csvRow("emp-001", "jane@vantage.com", "Jane Doe", "EMPLOYEE",
				"Senior Leadership;AI Capability Build"));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.getRowCategory()).isEqualTo(RowCategory.UPDATE);
		assertThat(rowResult.getOperations()).contains(ReconciliationOperation.ADD_COHORT_MEMBERSHIP);
		assertThat(rowResult.getOperations()).doesNotContain(ReconciliationOperation.NO_OP);
	}

	@Test
	void partialCohortDiffAddsAndRemoves() {
		Employee existing = employee("emp-001", "Jane Doe", "jane@vantage.com", "EMPLOYEE",
				List.of("leadership-2026", "managers-q2"));
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of(existing));

		CSVEmployeeCohortResponse response = reconcile(csvRow("emp-001", "jane@vantage.com", "Jane Doe", "EMPLOYEE",
				"Senior Leadership;Engineering"));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.getOperations()).contains(ReconciliationOperation.ADD_COHORT_MEMBERSHIP,
				ReconciliationOperation.REMOVE_COHORT_MEMBERSHIP);
	}

	@Test
	void unknownCohortOnUpdateWithExistingDbCohorts() {
		Employee existing = employee("emp-001", "Jane Doe", "jane@vantage.com", "EMPLOYEE",
				List.of("leadership-2026"));
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of(existing));

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "jane@vantage.com", "Jane Doe", "EMPLOYEE", "Senior Leadership;Does Not Exist"));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.isValid()).isFalse();
		assertThat(rowResult.getErrors()).extracting("code").contains("UNKNOWN_COHORT");
	}

	@Test
	void emailChangeIsConflictNotUpdate() {
		Employee existing = employee("emp-001", "Jane Doe", "jane@vantage.com", "EMPLOYEE", List.of());
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of(existing));

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "changed@vantage.com", "Jane Doe", "EMPLOYEE", null));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.isValid()).isFalse();
		assertThat(rowResult.getErrors()).extracting("code").contains("EMAIL_CONFLICT");
		assertThat(rowResult.getOperations()).contains(ReconciliationOperation.NO_OP);
		assertThat(rowResult.getOperations()).doesNotContain(ReconciliationOperation.UPDATE_EMPLOYEE);
	}

	@Test
	void nameChangeWithSameEmailIsUpdate() {
		Employee existing = employee("emp-001", "Jane Doe", "jane@vantage.com", "EMPLOYEE", List.of());
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of(existing));

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "jane@vantage.com", "Jane Smith", "EMPLOYEE", null));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.getRowCategory()).isEqualTo(RowCategory.UPDATE);
		assertThat(rowResult.getOperations()).contains(ReconciliationOperation.UPDATE_EMPLOYEE);
	}

	@Test
	void nullRoleDoesNotThrow() {
		Employee existing = employee("emp-001", "Jane Doe", "jane@vantage.com", null, List.of());
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of(existing));

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "jane@vantage.com", "Jane Doe", null, null));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.getRowCategory()).isEqualTo(RowCategory.SKIP_OP);
	}

	@Test
	void orphanCohortIdInDbDoesNotThrow() {
		Employee existing = employee("emp-001", "Jane Doe", "jane@vantage.com", "EMPLOYEE",
				List.of("unknown-cohort-id"));
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of(existing));

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "jane@vantage.com", "Jane Doe", "EMPLOYEE", "Senior Leadership"));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.isValid()).isFalse();
		assertThat(rowResult.getErrors()).extracting("code").contains("ORPHAN_COHORT_MEMBERSHIP");
	}

	@Test
	void invalidPassOneRowsSkipReconciliationPass() {
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of());

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "a@vantage.com", "A", null, null),
				csvRow("emp-001", "b@vantage.com", "B", null, null));

		assertThat(row(response, 2).getOperations()).contains(ReconciliationOperation.NO_OP);
		assertThat(row(response, 3).getOperations()).contains(ReconciliationOperation.NO_OP);
		assertThat(row(response, 2).getOperations()).doesNotContain(ReconciliationOperation.CREATE_EMPLOYEE);
	}

	@Test
	void emptyCsvCohortsOnExistingEmployeeMarksRemove() {
		Employee existing = employee("emp-001", "Jane Doe", "jane@vantage.com", "EMPLOYEE",
				List.of("leadership-2026"));
		when(employeeRepository.findAllById(Set.of("emp-001"))).thenReturn(List.of(existing));

		CSVEmployeeCohortResponse response = reconcile(
				csvRow("emp-001", "jane@vantage.com", "Jane Doe", "EMPLOYEE", null));
		CsvEmployeeCohortRowResult rowResult = row(response, 2);

		assertThat(rowResult.getOperations()).contains(ReconciliationOperation.REMOVE_COHORT_MEMBERSHIP);
	}

	private CSVEmployeeCohortResponse reconcile(CSVEmployeeCohortDto... rows) {
		return fileUploadService.reconcileRows(List.of(rows), TENANT);
	}

	private static CSVEmployeeCohortDto csvRow(String employeeId, String email, String name, String role,
			String cohortNames) {
		CSVEmployeeCohortDto dto = new CSVEmployeeCohortDto();
		dto.setEmployeeId(employeeId);
		dto.setEmail(email);
		dto.setName(name);
		dto.setRole(role);
		dto.setCohortNames(cohortNames);
		return dto;
	}

	private static Employee employee(String id, String name, String email, String role, List<String> cohortIds) {
		return new Employee(id, TENANT, cohortIds, name, email, role);
	}

	private static TenantCohort cohort(String cohortId, String cohortName) {
		return new TenantCohort(cohortId, cohortName, TENANT);
	}

	private static CsvEmployeeCohortRowResult row(CSVEmployeeCohortResponse response, int rowNumber) {
		return response.getRows().stream().filter(r -> r.getRowNumber() == rowNumber).findFirst().orElseThrow();
	}
}
