package com.securetransfer.platform.user.repository;

import com.securetransfer.platform.user.entity.Entreprise;
import com.securetransfer.platform.user.entity.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    Optional<Entreprise> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT e FROM Entreprise e
        WHERE (:kycStatus IS NULL OR e.kycStatus = :kycStatus)
        ORDER BY e.createdAt DESC
        """)
    Page<Entreprise> findAllFiltered(@Param("kycStatus") KycStatus kycStatus, Pageable pageable);
}