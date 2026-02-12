package com.example.paymentservice.adapter.persistence;


import com.example.paymentservice.domain.model.Transaction;
import com.example.paymentservice.entity.TransactionEntity;
import com.example.paymentservice.entity.TransactionType;
import com.example.paymentservice.mapper.TransactionMapper;
import com.example.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final SpringDataTransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public List<Transaction> findByPaymentId(Long paymentId) {
        return transactionRepository.findByPaymentId(paymentId)
                .stream()
                .map(transactionMapper::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findByType(TransactionType type) {
        return transactionRepository.findByType(type)
                .stream()
                .map(transactionMapper::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findByPaymentIdOrderByTimestampDesc(Long paymentId) {
        return transactionRepository.findByPaymentIdOrderByTimestampDesc(paymentId)
                .stream()
                .map(transactionMapper::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findAll() {
        return transactionRepository.findAll()
                .stream()
                .map(transactionMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id).map(transactionMapper::toDomain);
    }
}

interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findByPaymentId(Long paymentId);

    List<TransactionEntity> findByType(TransactionType type);

    List<TransactionEntity> findByPaymentIdOrderByTimestampDesc(Long paymentId);

}
