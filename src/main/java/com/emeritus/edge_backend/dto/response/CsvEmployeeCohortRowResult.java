package com.emeritus.edge_backend.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.emeritus.edge_backend.dto.request.CSVEmployeeCohortDto;

public class CsvEmployeeCohortRowResult {

	private int rowNumber;
	private boolean valid;
	private List<ReconciliationOperation> operations;
	private List<RowFieldError> errors;
	private CSVEmployeeCohortDto data;
	private RowCategory rowCategory; 

	public CsvEmployeeCohortRowResult(int rowNumber, boolean valid, List<ReconciliationOperation> operations,
			List<RowFieldError> errors, CSVEmployeeCohortDto data, RowCategory rowCategory) {
		this.rowNumber = rowNumber;
		this.valid = valid;
		this.operations = operations != null ? new ArrayList<>(operations) : new ArrayList<>();
		this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
		this.data = data;
		this.rowCategory = rowCategory;
	}

	public void addOperation(ReconciliationOperation operation) {
		if (operation == null) {
			return;
		}
		if (operations == null) {
			operations = new ArrayList<>();
		}
		if (operation != ReconciliationOperation.NO_OP) {
			operations.remove(ReconciliationOperation.NO_OP);
		}
		if (!operations.contains(operation)) {
			operations.add(operation);
		}
	}

	public void addError(RowFieldError error) {
		if (error == null) {
			return;
		}
		if (errors == null) {
			errors = new ArrayList<>();
		}
		errors.add(error);
		valid = false;
		if (operations == null) {
			operations = new ArrayList<>();
		}
		operations.clear();
		operations.add(ReconciliationOperation.NO_OP);
	}

	public int getRowNumber() {
		return rowNumber;
	}

	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	public CSVEmployeeCohortDto getData() {
		return data;
	}

	public void setData(CSVEmployeeCohortDto data) {
		this.data = data;
	}

	public List<RowFieldError> getErrors() {
		return errors;
	}

	public void setErrors(List<RowFieldError> errors) {
		this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public List<ReconciliationOperation> getOperations() {
		return operations;
	}

	public void setOperations(List<ReconciliationOperation> operations) {
		this.operations = operations != null ? new ArrayList<>(operations) : new ArrayList<>();
	}

	public RowCategory getRowCategory() {
		return rowCategory;
	}

	public void setRowCategory(RowCategory rowCategory) {
		this.rowCategory = rowCategory;
	}
	
}
