package com.expensetracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.expensetracker.dto.CategorySumDto;
import com.expensetracker.dto.MonthlyTrendDto;
import com.expensetracker.dto.SummaryDto;
import com.expensetracker.entity.Expense;
import com.expensetracker.service.ExpenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
public class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseService expenseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/expenses should return 201 Created when payload is valid")
    void testCreateExpense_ValidRequest() throws Exception {
        Expense requestBody = Expense.builder()
                .amount(new BigDecimal("45.00"))
                .category("Entertainment")
                .expenseDate(LocalDate.of(2026, 6, 10))
                .build();

        Expense savedRecord = Expense.builder()
                .id(UUID.randomUUID())
                .amount(new BigDecimal("45.00"))
                .category("Entertainment")
                .expenseDate(LocalDate.of(2026, 6, 10))
                .build();

        when(expenseService.createExpense(any(Expense.class))).thenReturn(savedRecord);

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(45.00))
                .andExpect(jsonPath("$.category").value("Entertainment"))
                .andExpect(jsonPath("$.expenseDate").value("2026-06-10"));
    }

    @Test
    @DisplayName("POST /api/expenses should return 400 Bad Request if expense amount is negative")
    void testCreateExpense_ValidationFails_NegativeAmount() throws Exception {
        Expense invalidBody = Expense.builder()
                .amount(new BigDecimal("-10.00"))
                .category("Entertainment")
                .expenseDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Expense amount must be greater than zero")));
    }

    @Test
    @DisplayName("POST /api/expenses should return 400 Bad Request if category is blank")
    void testCreateExpense_ValidationFails_BlankCategory() throws Exception {
        Expense invalidBody = Expense.builder()
                .amount(new BigDecimal("12.50"))
                .category("")
                .expenseDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Category must not be blank")));
    }

    @Test
    @DisplayName("POST /api/expenses should return 400 Bad Request if amount is null")
    void testCreateExpense_ValidationFails_NullAmount() throws Exception {
        Expense invalidBody = Expense.builder()
                .amount(null)
                .category("Food")
                .expenseDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Expense amount must not be null")));
    }

    @Test
    @DisplayName("POST /api/expenses should return 400 Bad Request if expenseDate is null")
    void testCreateExpense_ValidationFails_NullDate() throws Exception {
        Expense invalidBody = Expense.builder()
                .amount(new BigDecimal("15.00"))
                .category("Food")
                .expenseDate(null)
                .build();

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Expense date must not be null")));
    }

    @Test
    @DisplayName("GET /api/expenses should support category and monthYear parameters")
    void testGetExpenses_WithFilters() throws Exception {
        when(expenseService.getExpenses(eq("Food"), eq("2026-06"))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/expenses")
                .param("category", "Food")
                .param("monthYear", "2026-06"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/expenses/{id} should update and return updated expense")
    void testUpdateExpense_Success() throws Exception {
        UUID id = UUID.randomUUID();
        Expense requestBody = Expense.builder()
                .amount(new BigDecimal("50.00"))
                .category("Food")
                .expenseDate(LocalDate.of(2026, 6, 12))
                .build();

        Expense updated = Expense.builder()
                .id(id)
                .amount(new BigDecimal("50.00"))
                .category("Food")
                .expenseDate(LocalDate.of(2026, 6, 12))
                .build();

        when(expenseService.updateExpense(eq(id), any(Expense.class))).thenReturn(updated);

        mockMvc.perform(put("/api/expenses/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.category").value("Food"));
    }

    @Test
    @DisplayName("PUT /api/expenses/{id} should return 404 if expense does not exist")
    void testUpdateExpense_NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        Expense requestBody = Expense.builder()
                .amount(new BigDecimal("50.00"))
                .category("Food")
                .expenseDate(LocalDate.of(2026, 6, 12))
                .build();

        when(expenseService.updateExpense(eq(id), any(Expense.class))).thenThrow(new NoSuchElementException("Expense not found"));

        mockMvc.perform(put("/api/expenses/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Expense not found"));
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} should return 204 No Content on success")
    void testDeleteExpense_Success() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(expenseService).deleteExpense(id);

        mockMvc.perform(delete("/api/expenses/{id}", id))
                .andExpect(status().isNoContent());

        verify(expenseService, times(1)).deleteExpense(id);
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} should return 404 Not Found if expense does not exist")
    void testDeleteExpense_NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NoSuchElementException("Expense not found")).when(expenseService).deleteExpense(id);

        mockMvc.perform(delete("/api/expenses/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Expense not found"));
    }

    @Test
    @DisplayName("GET /api/expenses/stats/summary should return dashboard metrics")
    void testGetSummary() throws Exception {
        SummaryDto summary = SummaryDto.builder()
                .totalSpend(new BigDecimal("1200.50"))
                .currentMonthSpend(new BigDecimal("350.25"))
                .remainingBudget(new BigDecimal("649.75"))
                .budgetLimit(new BigDecimal("1000.00"))
                .budgetPercentage(new BigDecimal("35.03"))
                .build();

        when(expenseService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/expenses/stats/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpend").value(1200.50))
                .andExpect(jsonPath("$.currentMonthSpend").value(350.25))
                .andExpect(jsonPath("$.remainingBudget").value(649.75))
                .andExpect(jsonPath("$.budgetLimit").value(1000.00))
                .andExpect(jsonPath("$.budgetPercentage").value(35.03));
    }

    @Test
    @DisplayName("GET /api/expenses/stats/category-breakdown should return breakdown list")
    void testGetCategoryBreakdown() throws Exception {
        CategorySumDto item = new CategorySumDto("Food", new BigDecimal("150.00"));
        when(expenseService.getCategoryBreakdown(eq("2026-06"))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/expenses/stats/category-breakdown")
                .param("monthYear", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Food"))
                .andExpect(jsonPath("$[0].totalAmount").value(150.00));
    }

    @Test
    @DisplayName("GET /api/expenses/stats/monthly-trends should return trends list")
    void testGetMonthlyTrends() throws Exception {
        MonthlyTrendDto trend = new MonthlyTrendDto("2026-06", new BigDecimal("450.00"));
        when(expenseService.getMonthlyTrends(6)).thenReturn(List.of(trend));

        mockMvc.perform(get("/api/expenses/stats/monthly-trends")
                .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monthYear").value("2026-06"))
                .andExpect(jsonPath("$[0].totalAmount").value(450.00));
    }
}
