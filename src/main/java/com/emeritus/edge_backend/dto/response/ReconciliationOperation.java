package com.emeritus.edge_backend.dto.response;

public enum ReconciliationOperation {

	CREATE_EMPLOYEE, //employee_id not in tenant
	UPDATE_EMPLOYEE, //exists; name / role changed (email change blocked — error instead)
	ADD_COHORT_MEMBERSHIP, //cohort in CSV, not on employee today
	REMOVE_COHORT_MEMBERSHIP, //cohort on employee today, not in CSV (replace mode)
	NO_OP //optional — row matches state exactly (or omit from list)
}
