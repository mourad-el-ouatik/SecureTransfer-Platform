// ParticulierRepository.java — dans user/repository/
package com.securetransfer.platform.user.repository;

import com.securetransfer.platform.user.entity.KycStatus;
import com.securetransfer.platform.user.entity.Particulier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParticulierRepository extends JpaRepository<Particulier, Long> {

    Optional<Particulier> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT p FROM Particulier p
        WHERE (:kycStatus IS NULL OR p.kycStatus = :kycStatus)
        ORDER BY p.createdAt DESC
        """)
    Page<Particulier> findAllFiltered(@Param("kycStatus") KycStatus kycStatus, Pageable pageable);
}

// Répétez le même pattern pour AgenceRepository et EntrepriseRepository
// en remplaçant "Particulier" par "Agence" ou "Entreprise"
