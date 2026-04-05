package com.finance.dto.request;

import com.finance.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required (INCOME or EXPENSE)")
    private TransactionType type;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
