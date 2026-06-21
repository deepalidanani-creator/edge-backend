package com.emeritus.edge_backend.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.emeritus.edge_backend.dto.request.CSVEmployeeCohortDto;
import com.emeritus.edge_backend.dto.response.CSVEmployeeCohortResponse;
import com.emeritus.edge_backend.service.FileUploadService;

@RestController
@RequestMapping("/v1/upload")
public class FileUploadController {
	
	private FileUploadService fileUploadService;
	
	// Assumption : Define max limit (e.g., 10 MB expressed in bytes)
	private final long MAX_FILE_SIZE = 10 * 1024 * 1024; // it can go to properties file
	private final String MAX_FILE_SIZE_ERROR = "File size can not exceed 10MB."; // it can go to message properties file with ref to actual size variable defined
	
	public FileUploadController(FileUploadService fileUploadService) {
		this.fileUploadService = fileUploadService;
	}
	
	@PostMapping("/emp-with-cohort-assignment")
	public ResponseEntity<?> uploadEmpCohortAssignmentCSV(
            @RequestHeader(value = "X-Tenant-Id", required = true) String tenantId,
            @RequestHeader(value = "X-User-Role", required = true) String userRole,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey, 
            @RequestParam MultipartFile file){
		
		
		ResponseEntity<?> validationError = validateUpload(userRole, file);
		
		if (validationError != null) {
	        return validationError;
	    }
		
		try
		{

		 List<CSVEmployeeCohortDto> dtos = fileUploadService.parseCSV(file);

		 CSVEmployeeCohortResponse response = fileUploadService.reconcileRows(dtos, tenantId);
		 if (response.isValid()) {
	            return ResponseEntity.ok(response);
		 }
		 else {
			 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		 }
		 
		}catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		            .body(Map.of("error", "Failed to parse CSV: " + ex.getMessage()));
		}

	}

	private ResponseEntity<?> validateUpload(String userRole, MultipartFile file) {
		if (!"LND_ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access Denied: Only LND_ADMIN can upload employee files."));
        }
		
		// Check 2: if file is not sent and Check 3: Empty File Validation
		if(file == null || file.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "File is emplty"));
		}
		
		// Check 4: File Extension Validation, Trade off , this can be improved by using Apache Tikka
	    String filename = file.getOriginalFilename();
	   
	    if (filename == null || !filename.regionMatches(true, filename.length() - 4, ".csv", 0, 4)) { //case insensitive comparison
	    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Only .csv files are allowed."));
	    }

		//Check 5: Size Validation , Trade off : this should go to the properties file and dev, uat and prod properties file will be different
	    if(file.getSize() > MAX_FILE_SIZE) {
	    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", MAX_FILE_SIZE_ERROR));
	    }

	    return null;
	}

	
	

}
