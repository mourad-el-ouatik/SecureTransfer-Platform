package com.securetransfer.platform.transaction.repository;

import com.securetransfer.platform.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdempotencyKey(String key);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.senderId = :senderId AND t.createdAt >= :startDate")
    long countBySenderIdAndCreatedAtAfter(@Param("senderId") Long senderId,
            @Param("startDate") LocalDateTime startDate);
}
