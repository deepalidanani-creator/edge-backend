package com.emeritus.edge_backend.dto.response;

public class RowFieldError {

	String field;    // "email", "cohort_ids", "employee_id"
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	String code;     // "REQUIRED_FIELD_MISSING", "UNKNOWN_COHORT"
	String message;  // human-readable for tooltip / inline text

}
