package com.emeritus.edge_backend.dto.response;

import java.util.List;

public class CSVEmployeeCohortResponse {

	 boolean valid;
	 int totalRows;
	 List<CsvEmployeeCohortRowResult> rows;
	 
	 public boolean isValid() {
		return valid;
	}
	 public void setValid(boolean valid) {
		 this.valid = valid;
	 }
	 public int getTotalRows() {
		 return totalRows;
	 }
	 public void setTotalRows(int totalRows) {
		 this.totalRows = totalRows;
	 }
	 public List<CsvEmployeeCohortRowResult> getRows() {
		 return rows;
	 }
	 public void setRows(List<CsvEmployeeCohortRowResult> rows) {
		 this.rows = rows;
	 }
	
}

