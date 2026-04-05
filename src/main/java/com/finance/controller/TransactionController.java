package com.finance.controller;

import com.finance.dto.request.TransactionRequest;
import com.finance.dto.response.ApiResponse;
import com.finance.dto.response.TransactionResponse;
import com.finance.enums.TransactionType;
import com.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Financial record management")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Create a transaction — ADMIN, ANALYST only")
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created", transactionService.createTransaction(request)));
    }

    @Operation(summary = "List all transactions with optional filters")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions(
            @Parameter(description = "Filter by type: INCOME or EXPENSE")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter by category name (case-insensitive)")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter from date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Filter to date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved",
                transactionService.getAllTransactions(type, category, from, to)));
    }

    @Operation(summary = "Get a transaction by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved",
                transactionService.getTransactionById(id)));
    }

    @Operation(summary = "Update a transaction — ADMIN, ANALYST only")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Transaction updated",
                transactionService.updateTransaction(id, request)));
    }

    @Operation(summary = "Soft-delete a transaction — ADMIN, ANALYST only")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted", null));
    }
}
