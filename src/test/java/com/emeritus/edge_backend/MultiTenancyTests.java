package com.emeritus.edge_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MultiTenancyTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSessionFromAnotherTenantReturnsNotFound() throws Exception {
        mockMvc.perform(get("/v1/sessions/1")
                .header("X-Tenant-Id", "apex-edu"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getAllocationsWithMismatchedTenantHeaderReturnsForbidden() throws Exception {
        mockMvc.perform(get("/v1/tenants/vantage-fi/allocations")
                .header("X-Tenant-Id", "apex-edu"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Access denied: X-Tenant-Id does not match the requested tenant."));
    }

    @Test
    void getAllocationsWithoutTenantHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/v1/tenants/vantage-fi/allocations"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("X-Tenant-Id header is required."));
    }

    @Test
    void globalCatalogueEndpointsDoNotRequireTenantHeader() throws Exception {
        mockMvc.perform(get("/v1/topics"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/v1/speakers"))
            .andExpect(status().isOk());
    }
}
