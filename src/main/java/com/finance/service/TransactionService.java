package com.finance.service;

import com.finance.dto.request.TransactionRequest;
import com.finance.dto.response.TransactionResponse;
import com.finance.enums.TransactionType;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {
    TransactionResponse createTransaction(TransactionRequest request);
    TransactionResponse getTransactionById(Long id);
    List<TransactionResponse> getAllTransactions(
            TransactionType type, String category, LocalDate from, LocalDate to);
    TransactionResponse updateTransaction(Long id, TransactionRequest request);
    void deleteTransaction(Long id);
}
