package com.expensetracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.expensetracker.entity.BudgetSettings;
import com.expensetracker.service.BudgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
public class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BudgetService budgetService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/budget-settings should return the active budget configuration")
    void testGetBudgetSettings() throws Exception {
        BudgetSettings config = new BudgetSettings(BudgetService.BUDGET_ID, new BigDecimal("1200.00"));
        when(budgetService.getBudgetSettings()).thenReturn(config);

        mockMvc.perform(get("/api/budget-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000000"))
                .andExpect(jsonPath("$.monthlyLimit").value(1200.00));
    }

    @Test
    @DisplayName("PUT /api/budget-settings should update configurations with positive amounts")
    void testUpdateBudgetSettings_Success() throws Exception {
        BudgetSettings updatedConfig = new BudgetSettings(BudgetService.BUDGET_ID, new BigDecimal("1500.00"));
        when(budgetService.updateBudgetSettings(any(BigDecimal.class))).thenReturn(updatedConfig);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("monthlyLimit", 1500.00);

        mockMvc.perform(put("/api/budget-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyLimit").value(1500.00));
    }

    @Test
    @DisplayName("PUT /api/budget-settings should return 400 Bad Request if monthly limit parameter is missing")
    void testUpdateBudgetSettings_MissingParameter() throws Exception {
        Map<String, Object> requestBody = new HashMap<>(); // Empty parameters

        mockMvc.perform(put("/api/budget-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Field 'monthlyLimit' is required")));
    }

    @Test
    @DisplayName("PUT /api/budget-settings should return 400 Bad Request if monthly limit parameter is null")
    void testUpdateBudgetSettings_NullParameter() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("monthlyLimit", null);

        mockMvc.perform(put("/api/budget-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Field 'monthlyLimit' is required")));
    }

    @Test
    @DisplayName("PUT /api/budget-settings should return 400 Bad Request if monthly limit is non-numeric")
    void testUpdateBudgetSettings_InvalidFormat() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("monthlyLimit", "invalid-amount");

        mockMvc.perform(put("/api/budget-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid monthlyLimit value")));
    }

    @Test
    @DisplayName("PUT /api/budget-settings should return 400 Bad Request if monthly limit is zero or negative")
    void testUpdateBudgetSettings_NegativeLimit() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("monthlyLimit", -10.00);

        mockMvc.perform(put("/api/budget-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Monthly budget limit must be greater than zero")));
    }
}
