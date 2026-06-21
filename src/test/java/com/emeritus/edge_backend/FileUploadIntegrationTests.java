package com.emeritus.edge_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadIntegrationTests {

	private static final Path SAMPLES = Path.of("samples");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void happyCsvUploadReturnsOkWithAllRowsValid() throws Exception {
		byte[] csv = Files.readAllBytes(SAMPLES.resolve("upload-happy.csv"));

		mockMvc.perform(multipart("/v1/upload/emp-with-cohort-assignment")
				.file(new MockMultipartFile("file", "upload-happy.csv", "text/csv", csv))
				.header("X-Tenant-Id", "vantage-fi")
				.header("X-User-Role", "LND_ADMIN")
				.header("Idempotency-Key", "integration-happy-001"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.totalRows").value(3))
			.andExpect(jsonPath("$.rows[0].rowNumber").value(2))
			.andExpect(jsonPath("$.rows[0].rowCategory").value("CREATE"))
			.andExpect(jsonPath("$.rows[1].rowCategory").value("SKIP_OP"))
			.andExpect(jsonPath("$.rows[2].rowCategory").value("CREATE"));
	}

	@Test
	void updateCsvUploadReturnsOkWithUpdateRow() throws Exception {
		byte[] csv = Files.readAllBytes(SAMPLES.resolve("upload-update.csv"));

		mockMvc.perform(multipart("/v1/upload/emp-with-cohort-assignment")
				.file(new MockMultipartFile("file", "upload-update.csv", "text/csv", csv))
				.header("X-Tenant-Id", "vantage-fi")
				.header("X-User-Role", "LND_ADMIN")
				.header("Idempotency-Key", "integration-update-001"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.rows[0].rowCategory").value("UPDATE"))
			.andExpect(jsonPath("$.rows[0].operations[?(@ == 'UPDATE_EMPLOYEE')]").exists())
			.andExpect(jsonPath("$.rows[0].operations[?(@ == 'ADD_COHORT_MEMBERSHIP')]").exists())
			.andExpect(jsonPath("$.rows[0].operations[?(@ == 'REMOVE_COHORT_MEMBERSHIP')]").exists());
	}

	@Test
	void errorCsvUploadReturnsBadRequestWithPartialRows() throws Exception {
		byte[] csv = Files.readAllBytes(SAMPLES.resolve("upload-errors.csv"));

		mockMvc.perform(multipart("/v1/upload/emp-with-cohort-assignment")
				.file(new MockMultipartFile("file", "upload-errors.csv", "text/csv", csv))
				.header("X-Tenant-Id", "vantage-fi")
				.header("X-User-Role", "LND_ADMIN")
				.header("Idempotency-Key", "integration-errors-001"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.totalRows").value(4))
			.andExpect(jsonPath("$.rows[0].errors[0].code").value("DUPLICATE_EMPLOYEE"))
			.andExpect(jsonPath("$.rows[2].errors[0].code").value("UNKNOWN_COHORT"))
			.andExpect(jsonPath("$.rows[3].errors[0].code").value("EMAIL_CONFLICT"));
	}

	@Test
	void nonAdminRoleReturnsForbidden() throws Exception {
		byte[] csv = Files.readAllBytes(SAMPLES.resolve("upload-happy.csv"));

		mockMvc.perform(multipart("/v1/upload/emp-with-cohort-assignment")
				.file(new MockMultipartFile("file", "upload-happy.csv", "text/csv", csv))
				.header("X-Tenant-Id", "vantage-fi")
				.header("X-User-Role", "EMPLOYEE")
				.header("Idempotency-Key", "integration-forbidden-001"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error")
				.value("Access Denied: Only LND_ADMIN can upload employee files."));
	}

	@Test
	void emptyFileReturnsBadRequest() throws Exception {
		mockMvc.perform(multipart("/v1/upload/emp-with-cohort-assignment")
				.file(new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]))
				.header("X-Tenant-Id", "vantage-fi")
				.header("X-User-Role", "LND_ADMIN")
				.header("Idempotency-Key", "integration-empty-001"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("File is emplty"));
	}

	@Test
	void nonCsvExtensionReturnsBadRequest() throws Exception {
		mockMvc.perform(multipart("/v1/upload/emp-with-cohort-assignment")
				.file(new MockMultipartFile("file", "employees.txt", "text/plain", "x".getBytes()))
				.header("X-Tenant-Id", "vantage-fi")
				.header("X-User-Role", "LND_ADMIN")
				.header("Idempotency-Key", "integration-ext-001"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Only .csv files are allowed."));
	}

	@Test
	void missingTenantHeaderReturnsBadRequest() throws Exception {
		byte[] csv = Files.readAllBytes(SAMPLES.resolve("upload-happy.csv"));

		mockMvc.perform(multipart("/v1/upload/emp-with-cohort-assignment")
				.file(new MockMultipartFile("file", "upload-happy.csv", "text/csv", csv))
				.header("X-User-Role", "LND_ADMIN")
				.header("Idempotency-Key", "integration-notenant-001"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("X-Tenant-Id header is required."));
	}
}
