package com.securetransfer.platform.kyc.repository;

import com.securetransfer.platform.kyc.entity.KycDocument;
import com.securetransfer.platform.kyc.entity.KycDocumentState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByUserIdAndUserType(Long userId, String userType);

    List<KycDocument> findByKycState(KycDocumentState state);

    @Query("SELECT k FROM KycDocument k WHERE k.userId = :userId AND k.userType = :userType AND k.kycState = :state")
    List<KycDocument> findByUserAndState(Long userId, String userType, KycDocumentState state);

    Optional<KycDocument> findByFileHash(String fileHash);
}