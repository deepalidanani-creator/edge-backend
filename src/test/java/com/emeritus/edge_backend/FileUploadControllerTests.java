package com.emeritus.edge_backend;

import com.emeritus.edge_backend.controller.FileUploadController;
import com.emeritus.edge_backend.service.FileUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTests {

	@Mock
	private FileUploadService fileUploadService;

	@InjectMocks
	private FileUploadController controller;

	@Test
	void nonAdminRoleGetsUploadSpecificForbiddenMessage() {
		MockMultipartFile file = new MockMultipartFile("file", "employees.csv", "text/csv",
				"employee_id,email,name\n".getBytes());

		ResponseEntity<?> response = controller.uploadEmpCohortAssignmentCSV("vantage-fi", "EMPLOYEE", "key-1", file);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).isEqualTo(
				java.util.Map.of("error", "Access Denied: Only LND_ADMIN can upload employee files."));
	}
}
