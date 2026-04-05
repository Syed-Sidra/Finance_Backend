package com.finance.service.impl;

import com.finance.dto.request.TransactionRequest;
import com.finance.dto.response.TransactionResponse;
import com.finance.entity.Transaction;
import com.finance.entity.User;
import com.finance.enums.TransactionType;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.TransactionRepository;
import com.finance.service.TransactionService;
import com.finance.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User currentUser = userService.getCurrentUser();

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .date(request.getDate())
                .notes(request.getNotes())
                .deleted(false)
                .createdBy(currentUser)
                .build();

        return toResponse(transactionRepository.save(transaction));
    }

    @Override
    public TransactionResponse getTransactionById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<TransactionResponse> getAllTransactions(
            TransactionType type, String category, LocalDate from, LocalDate to) {

        List<Transaction> results;

        boolean hasType = type != null;
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasDateRange = from != null && to != null;

        if (hasType && hasDateRange) {
            results = transactionRepository.findByDeletedFalseAndTypeAndDateBetween(type, from, to);
        } else if (hasCategory && hasDateRange) {
            results = transactionRepository.findByDeletedFalseAndCategoryIgnoreCaseAndDateBetween(category, from, to);
        } else if (hasType) {
            results = transactionRepository.findByDeletedFalseAndType(type);
        } else if (hasCategory) {
            results = transactionRepository.findByDeletedFalseAndCategoryIgnoreCase(category);
        } else if (hasDateRange) {
            results = transactionRepository.findByDeletedFalseAndDateBetween(from, to);
        } else {
            results = transactionRepository.findByDeletedFalse();
        }

        return results.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        Transaction transaction = findOrThrow(id);

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory().trim());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());

        return toResponse(transactionRepository.save(transaction));
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = findOrThrow(id);
        // Soft delete
        transaction.setDeleted(true);
        transactionRepository.save(transaction);
    }


    private Transaction findOrThrow(Long id) {
        return transactionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    public TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType())
                .category(t.getCategory())
                .date(t.getDate())
                .notes(t.getNotes())
                .createdByEmail(t.getCreatedBy().getEmail())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
