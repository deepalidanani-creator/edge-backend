package com.emeritus.edge_backend.service;

import com.emeritus.edge_backend.dto.request.CSVEmployeeCohortDto;
import com.emeritus.edge_backend.dto.response.CSVEmployeeCohortResponse;
import com.emeritus.edge_backend.dto.response.CsvEmployeeCohortRowResult;
import com.emeritus.edge_backend.dto.response.ReconciliationOperation;
import com.emeritus.edge_backend.dto.response.RowCategory;
import com.emeritus.edge_backend.dto.response.RowFieldError;
import com.emeritus.edge_backend.entity.Employee;
import com.emeritus.edge_backend.entity.TenantCohort;
import com.emeritus.edge_backend.repository.EmployeeRepository;
import com.emeritus.edge_backend.repository.TenantCohortRepository;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class FileUploadService {

	enum ALLOWED_ROLES {
		LND_ADMIN, EMPLOYEE
	}

	private final EmployeeRepository employeeRepository;
	private final TenantCohortRepository tenantCohortRepository;

	public FileUploadService(EmployeeRepository employeeRepository, TenantCohortRepository tenantCohortRepository) {
		this.employeeRepository = employeeRepository;
		this.tenantCohortRepository = tenantCohortRepository;
	}

	public List<CSVEmployeeCohortDto> parseCSV(MultipartFile file) throws Exception {
		try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			return new CsvToBeanBuilder<CSVEmployeeCohortDto>(reader).withType(CSVEmployeeCohortDto.class)
					.withIgnoreLeadingWhiteSpace(true).build().parse();
		}
	}

	public CSVEmployeeCohortResponse reconcileRows(List<CSVEmployeeCohortDto> rows, String tenantId) {

		CSVEmployeeCohortResponse response = new CSVEmployeeCohortResponse();
		Map<Integer, CsvEmployeeCohortRowResult> rowNumVsRowResultsMap = new HashMap<>();

		Map<String, CSVEmployeeCohortDto> distinctEmpIdRowNumMap = new HashMap<>();
		Map<String, String> distinctEmailIdEmployeeIdMap = new HashMap<>();
		Integer rowNumber;
		CSVEmployeeCohortDto csvRow;
		CSVEmployeeCohortDto alreadyAddedCSVRow;
		CsvEmployeeCohortRowResult alreadyAddedRowResult;
		CsvEmployeeCohortRowResult rowResult;
		for (int i = 0; i < rows.size(); i++) {
			rowNumber = i + 2; // CSV line 1 is the header
			csvRow = rows.get(i);
			csvRow.setRowNumber(rowNumber);
			rowResult = new CsvEmployeeCohortRowResult(rowNumber, true, null, null, csvRow, RowCategory.SKIP_OP);
			rowNumVsRowResultsMap.put(rowNumber, rowResult);

			if (csvRow.hasRequiredFields()) {
				System.out.println("this csv row has all the Mandatory fields. rowNumber: " + rowNumber);

				if (!distinctEmpIdRowNumMap.containsKey(csvRow.getEmployeeId())) {
					distinctEmpIdRowNumMap.put(csvRow.getEmployeeId(), csvRow);
				} else {
					alreadyAddedCSVRow = distinctEmpIdRowNumMap.get(csvRow.getEmployeeId());
					alreadyAddedRowResult = rowNumVsRowResultsMap.get(alreadyAddedCSVRow.getRowNumber());

					markRowForDuplicateEmployeeId(rowResult);
					markRowForDuplicateEmployeeId(alreadyAddedRowResult);

					System.out.println("Duplicate Employee Id found rowNumber: " + rowNumber + " Prev Row Number : "
							+ alreadyAddedCSVRow.getRowNumber());
				}

				if (!distinctEmailIdEmployeeIdMap.containsKey(csvRow.getEmail())) {
					distinctEmailIdEmployeeIdMap.put(csvRow.getEmail(), csvRow.getEmployeeId());
				} else {

					alreadyAddedCSVRow = distinctEmpIdRowNumMap
							.get(distinctEmailIdEmployeeIdMap.get(csvRow.getEmail()));
					alreadyAddedRowResult = rowNumVsRowResultsMap.get(alreadyAddedCSVRow.getRowNumber());
					markRowForDuplicateEmailId(rowResult);
					markRowForDuplicateEmailId(alreadyAddedRowResult);
					System.out.println("Duplicate Email Id found rowNumber: " + rowNumber + " Prev Row Number : "
							+ alreadyAddedCSVRow.getRowNumber());
				}

			} else {
				System.out.println(
						"Can't proceed further as this csv row has missing mandatory fields rowNumber: " + rowNumber);

				addMissingFieldsErrors(rowResult, csvRow, rowNumber);
			}
		}

		System.out.println(
				"Basic data validation complete. Proceeding to check if new employee present + suggest operations for update/delete for existing emlployee details and membership details");
		Set<String> distinctEmployeeIds = distinctEmpIdRowNumMap.keySet();
		Map<String, TenantCohort> tenantCohortByCohortName = new HashMap<>();
		Map<String, TenantCohort> tenantCohortByCohortId = new HashMap<>();
		loadTenantCohortCatalog(tenantId, tenantCohortByCohortName, tenantCohortByCohortId);
		Set<String> tenantCohortNames = tenantCohortByCohortName.keySet();
		Map<String, Employee> existingEmpIdEmployeesMap = loadEmployeesByIds(distinctEmployeeIds, tenantId);
		Employee existingEmployeeInfo = null;

		for (int i = 0; i < rows.size(); i++) {
			csvRow = rows.get(i);
			rowNumber = i + 2;
			rowResult = rowNumVsRowResultsMap.get(rowNumber);
			if (!isEligibleForReconciliation(rowResult)) {
				continue;
			}
			if (existingEmpIdEmployeesMap.containsKey(csvRow.getEmployeeId())) {
				System.out.println("Employee already present. Checking further details");
				existingEmployeeInfo = existingEmpIdEmployeesMap.get(csvRow.getEmployeeId());
				reconcileExistingEmployee(rowResult, csvRow, existingEmployeeInfo, tenantCohortNames,
						tenantCohortByCohortId);
			} else {
				System.out.println("New Employee request.");

				if (csvRow.getCohortNameList().isEmpty()) {
					System.out.println("New Employee without Cohort Membership");
					markCreateNewEmployeeInRow(rowResult);

				} else {
					System.out.println("New Employee with Cohort Names mentioned");
					if (tenantCohortNames.containsAll(csvRow.getCohortNameList())) {
						System.out.println("All cohort names are valid for Tenant id, good to assign.");
						markCreateNewEmployeeCohortMembershipInRow(rowResult);

					} else {
						System.out.println("Cohort mentioned is not present for the Tenant. Marking for Error.");
						markRowForUnknownCohortForTenant(rowResult);
					}
				}
			}
		}
		response.setValid(rowNumVsRowResultsMap.values().stream().allMatch(CsvEmployeeCohortRowResult::isValid));
		response.setTotalRows(rows.size());
		response.setRows(rowNumVsRowResultsMap.values().stream()
				.sorted(Comparator.comparingInt(CsvEmployeeCohortRowResult::getRowNumber)).toList());
		return response;
	}

	private void reconcileExistingEmployee(CsvEmployeeCohortRowResult rowResult, CSVEmployeeCohortDto csvRow,
			Employee existingEmployeeInfo, Set<String> tenantCohortNames,
			Map<String, TenantCohort> tenantCohortByCohortId) {
		List<String> csvCohortNames = csvRow.getCohortNameList();

		if (!csvCohortNames.isEmpty() && !tenantCohortNames.containsAll(csvCohortNames)) {
			markRowForUnknownCohortForTenant(rowResult);
			return;
		}

		List<String> dbCohortNames = resolveEmployeeCohortNames(existingEmployeeInfo, tenantCohortByCohortId, rowResult);
		if (dbCohortNames == null) {
			return;
		}

		if (csvCohortNames.isEmpty()) {
			if (!dbCohortNames.isEmpty()) {
				markRemoveCohortMembership(rowResult);
			}
		} else {
			markCohortMembershipChanges(rowResult, csvCohortNames, dbCohortNames);
		}

		checkEmployeeDetailsAndMarkOperation(rowResult, csvRow, existingEmployeeInfo);
	}

	private List<String> resolveEmployeeCohortNames(Employee existingEmployeeInfo,
			Map<String, TenantCohort> tenantCohortByCohortId, CsvEmployeeCohortRowResult rowResult) {
		List<String> employeeExistingCohortNames = new ArrayList<>();
		if (existingEmployeeInfo.getCohortIds() == null || existingEmployeeInfo.getCohortIds().isEmpty()) {
			return employeeExistingCohortNames;
		}
		for (String existingCohortId : existingEmployeeInfo.getCohortIds()) {
			TenantCohort cohort = tenantCohortByCohortId.get(existingCohortId);
			if (cohort == null) {
				markRowForOrphanCohortMembership(rowResult, existingCohortId);
				return null;
			}
			employeeExistingCohortNames.add(cohort.getCohortName());
		}
		return employeeExistingCohortNames;
	}

	private void markCohortMembershipChanges(CsvEmployeeCohortRowResult rowResult, List<String> csvCohortNames,
			List<String> dbCohortNames) {
		Set<String> csvCohortSet = new HashSet<>(csvCohortNames);
		Set<String> dbCohortSet = new HashSet<>(dbCohortNames);
		if (csvCohortSet.equals(dbCohortSet)) {
			return;
		}
		if (!dbCohortSet.containsAll(csvCohortSet)) {
			markAddCohortMembership(rowResult);
		}
		if (!csvCohortSet.containsAll(dbCohortSet)) {
			markRemoveCohortMembership(rowResult);
		}
	}

	private boolean isEligibleForReconciliation(CsvEmployeeCohortRowResult rowResult) {
		return rowResult.isValid();
	}

	private void markCreateNewEmployeeInRow(CsvEmployeeCohortRowResult rowResult) {
		rowResult.setRowCategory(RowCategory.CREATE);
		rowResult.addOperation(ReconciliationOperation.CREATE_EMPLOYEE);
	}

	private void markCreateNewEmployeeCohortMembershipInRow(CsvEmployeeCohortRowResult rowResult) {
		rowResult.setRowCategory(RowCategory.CREATE);
		rowResult.addOperation(ReconciliationOperation.CREATE_EMPLOYEE);
		rowResult.addOperation(ReconciliationOperation.ADD_COHORT_MEMBERSHIP);
	}

	private void checkEmployeeDetailsAndMarkOperation(CsvEmployeeCohortRowResult rowResult, CSVEmployeeCohortDto csvRow,
			Employee existingEmployeeInfo) {
		if (!csvRow.getEmail().equals(existingEmployeeInfo.getEmailid())) {
			markRowForEmailConflict(rowResult, existingEmployeeInfo.getEmailid());
			return;
		}
		if (csvRow.getName().equals(existingEmployeeInfo.getName())
				&& Objects.equals(csvRow.getRole(), existingEmployeeInfo.getRole())) {
			System.out.println("Nothing changed for employee details");
			if (!hasReconciliationOperations(rowResult)) {
				markNoOp(rowResult);
			}
		} else {
			System.out.println("Name or role changed, marking for employee update");
			markUpdateEmployee(rowResult);
		}
	}

	private void addMissingFieldsErrors(CsvEmployeeCohortRowResult rowResult, CSVEmployeeCohortDto row, int rowNumber) {
		
		rowResult.setRowCategory(RowCategory.CORRECTION);
		rowResult.getOperations().add(ReconciliationOperation.NO_OP);
		if (!row.isValidEmployeeId()) {
			RowFieldError error = new RowFieldError();
			error.setField("employeeId");
			error.setCode("REQUIRED_FIELD_MISSING");
			error.setMessage("employee_id is required.");
			rowResult.addError(error);
		}

		if (!row.isValidEmail()) {
			RowFieldError error = new RowFieldError();
			error.setField("emailId");
			error.setCode("REQUIRED_FIELD_MISSING");
			error.setMessage("email_id is required.");
			rowResult.addError(error);
		}

		if (!row.isValidName()) {
			RowFieldError error = new RowFieldError();
			error.setField("name");
			error.setCode("REQUIRED_FIELD_MISSING");
			error.setMessage("name is required.");
			rowResult.addError(error);
		}
	}

	private void markRowForDuplicateEmployeeId(CsvEmployeeCohortRowResult rowResult) {

		RowFieldError error = new RowFieldError();
		error.setField("employeeId");
		error.setCode("DUPLICATE_EMPLOYEE");
		error.setMessage("Duplicate Employee Id.");
		addError(rowResult, error);
	}

	private void markRowForDuplicateEmailId(CsvEmployeeCohortRowResult rowResult) {
		RowFieldError error = new RowFieldError();
		error.setField("emailId");
		error.setCode("DUPLICATE_EMAIL");
		error.setMessage("Duplicate Email Id.");
		addError(rowResult, error);
	}

	private void markRowForUnknownCohortForTenant(CsvEmployeeCohortRowResult rowResult) {
		RowFieldError error = new RowFieldError();
		error.setField("cohortName");
		error.setCode("UNKNOWN_COHORT");
		error.setMessage("Unknown Cohorts");
		addError(rowResult, error);
	}

	private void markRowForEmailConflict(CsvEmployeeCohortRowResult rowResult, String existingEmailId) {
		RowFieldError error = new RowFieldError();
		error.setField("emailId");
		error.setCode("EMAIL_CONFLICT");
		error.setMessage("Email cannot be changed for an existing employee. email id already exisist is " + existingEmailId);
		addError(rowResult, error);
	}

	private void markRowForOrphanCohortMembership(CsvEmployeeCohortRowResult rowResult, String cohortId) {
		RowFieldError error = new RowFieldError();
		error.setField("cohortName");
		error.setCode("ORPHAN_COHORT_MEMBERSHIP");
		error.setMessage("Employee is assigned to unknown cohort id: " + cohortId);
		addError(rowResult, error);
	}

	private void addError(CsvEmployeeCohortRowResult rowResult, RowFieldError error) {
		rowResult.addError(error);
		rowResult.setRowCategory(RowCategory.CORRECTION);
	}

	private boolean hasReconciliationOperations(CsvEmployeeCohortRowResult rowResult) {
		return rowResult.getOperations().stream().anyMatch(op -> op != ReconciliationOperation.NO_OP);
	}

	private void markNoOp(CsvEmployeeCohortRowResult rowResult) {
		rowResult.setRowCategory(RowCategory.SKIP_OP);
		rowResult.getOperations().clear();
		rowResult.addOperation(ReconciliationOperation.NO_OP);
	}

	private void markUpdateEmployee(CsvEmployeeCohortRowResult rowResult) {
		rowResult.addOperation(ReconciliationOperation.UPDATE_EMPLOYEE);
		rowResult.setRowCategory(RowCategory.UPDATE);
	}

	private void markRemoveCohortMembership(CsvEmployeeCohortRowResult rowResult) {
		rowResult.addOperation(ReconciliationOperation.REMOVE_COHORT_MEMBERSHIP);
		rowResult.setRowCategory(RowCategory.UPDATE);
	}

	private void markAddCohortMembership(CsvEmployeeCohortRowResult rowResult) {
		rowResult.addOperation(ReconciliationOperation.ADD_COHORT_MEMBERSHIP);
		rowResult.setRowCategory(RowCategory.UPDATE);
	}

	/** Step 1: load tenant cohort catalog — name → cohort and id → cohort. */
	private void loadTenantCohortCatalog(String tenantId, Map<String, TenantCohort> tenantCohortByCohortName,
			Map<String, TenantCohort> tenantCohortByCohortId) {
		for (TenantCohort cohort : tenantCohortRepository.findByTenantId(tenantId)) {
			tenantCohortByCohortName.put(cohort.getCohortName(), cohort);
			tenantCohortByCohortId.put(cohort.getCohortId(), cohort);
		}
	}

	/** Step 2: load existing employees referenced in CSV (this tenant only). */
	private Map<String, Employee> loadEmployeesByIds(Set<String> employeeIds, String tenantId) {
		Map<String, Employee> existingEmpIdEmployeesMap = new HashMap<>();
		if (employeeIds.isEmpty()) {
			return existingEmpIdEmployeesMap;
		}
		for (Employee employee : employeeRepository.findAllById(employeeIds)) {
			if (tenantId.equals(employee.getTenantId())) {
				existingEmpIdEmployeesMap.put(employee.getId(), employee);
			}
		}
		return existingEmpIdEmployeesMap;
	}
}
