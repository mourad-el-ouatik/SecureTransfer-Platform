package com.securetransfer.platform.user.repository;

import com.securetransfer.platform.user.entity.Agence;
import com.securetransfer.platform.user.entity.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AgenceRepository extends JpaRepository<Agence, Long> {

    Optional<Agence> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT a FROM Agence a
        WHERE (:kycStatus IS NULL OR a.kycStatus = :kycStatus)
        ORDER BY a.createdAt DESC
        """)
    Page<Agence> findAllFiltered(@Param("kycStatus") KycStatus kycStatus, Pageable pageable);
}