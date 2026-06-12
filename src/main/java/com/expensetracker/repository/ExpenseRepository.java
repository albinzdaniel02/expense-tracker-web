package com.expensetracker.repository;

import com.expensetracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    // Retrieve Overall Total Spent
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    BigDecimal sumAllExpenses();

    // Retrieve Spent for Specific Date Range
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumExpensesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // Category Aggregations DTO projection mapping
    @Query("SELECT e.category AS category, SUM(e.amount) AS totalAmount " +
           "FROM Expense e " +
           "WHERE e.expenseDate BETWEEN :start AND :end " +
           "GROUP BY e.category " +
           "ORDER BY totalAmount DESC")
    List<CategorySum> getCategoryBreakdown(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // Projection Interface for Category Aggregations
    interface CategorySum {
        String getCategory();
        BigDecimal getTotalAmount();
    }
}
