package com.emeritus.edge_backend.dto.request;

import com.opencsv.bean.CsvBindByName;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * One row from an employee/cohort CSV upload.
 *
 * <p>Expected header row (column order may vary; names must match):
 *
 * <pre>
 * employee_id,email,name,role,cohort_names,start_date
 * </pre>
 *
 * <p>Example data row:
 *
 * <pre>
 * emp-001,jane@vantage.com,Jane Doe,LND_ADMIN,"Senior Leadership;AI Capability Build",2026-01-15
 * </pre>
 *
 * <h2>Field rules</h2>
 * <ul>
 *   <li>{@code employee_id} — string, required, unique within the file</li>
 *   <li>{@code email} — string, required</li>
 *   <li>{@code name} — string, required</li>
 *   <li>{@code role} — string, optional (e.g. LND_ADMIN, EMPLOYEE)</li>
 *   <li>{@code cohort_names} — optional; semicolon-separated cohort names</li>
 *   <li>{@code start_date} — optional; ISO-8601 date ({@code yyyy-MM-dd})</li>
 * </ul>
 *
 * <p>Row-level uniqueness of {@code employee_id} and required-field checks are enforced
 * in {@code FileUploadService} after parsing, not inside this DTO.
 */
public class CSVEmployeeCohortDto {
	
	private int rowNumber;

    @CsvBindByName(column = "employee_id", required = true)
    private String employeeId;

    @CsvBindByName(column = "email", required = true)
    private String email;

    @CsvBindByName(column = "name", required = true)
    private String name;

    @CsvBindByName(column = "role")
    private String role;

    @CsvBindByName(column = "cohort_names")
    private String cohortNames;

    @CsvBindByName(column = "start_date")
    private String startDate;

    public CSVEmployeeCohortDto() {}

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /** Raw CSV value, e.g. {@code "Senior Leadership;AI Capability Build"}. */
    public String getCohortNames() {
        return cohortNames;
    }

    public void setCohortNames(String cohortNames) {
        this.cohortNames = cohortNames;
    }

    /** Raw ISO date string from CSV, e.g. {@code "2026-01-15"}. */
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    /** Parses {@link #cohortNames} into trimmed, non-empty cohort name tokens. */
    public List<String> getCohortNameList() {
        if (cohortNames == null || cohortNames.isBlank()) {
            return List.of();
        }
        return Arrays.stream(cohortNames.split(";"))
            .map(String::trim)
            .filter(token -> !token.isEmpty())
            .collect(Collectors.toList());
    }

    /** Parses {@link #startDate} as {@link LocalDate}; empty if absent or invalid. */
    public Optional<LocalDate> getStartDateAsLocalDate() {
        if (startDate == null || startDate.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(startDate.trim()));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }  
    

    public int getRowNumber() {
		return rowNumber;
	}

	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	public boolean hasRequiredFields() {
        return isValidEmployeeId()
            && isValidEmail()
            && isValidName();
    }
    
	public boolean isValidEmployeeId() {
		return employeeId != null && !employeeId.isBlank();
	}

	public boolean isValidEmail() {
		return email != null && !email.isBlank();
	}

	public boolean isValidName() {
		return name != null && !name.isBlank();
	}
}
