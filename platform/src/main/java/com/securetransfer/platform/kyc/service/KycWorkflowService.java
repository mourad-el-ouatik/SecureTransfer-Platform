package com.securetransfer.platform.kyc.service;

import com.securetransfer.platform.common.exception.BusinessException;
import com.securetransfer.platform.kyc.entity.KycDocument;
import com.securetransfer.platform.kyc.entity.KycDocumentState;
import com.securetransfer.platform.kyc.event.KycReviewedEvent;
import com.securetransfer.platform.kyc.event.KycSubmittedEvent;
import com.securetransfer.platform.kyc.repository.KycDocumentRepository;
import com.securetransfer.platform.user.entity.KycStatus;
import com.securetransfer.platform.user.repository.AgenceRepository;
import com.securetransfer.platform.user.repository.EntrepriseRepository;
import com.securetransfer.platform.user.repository.ParticulierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycWorkflowService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/jpg", "application/pdf"
    );
    private static final String STORAGE_DIR = "kyc-documents"; // outside webroot

    private final KycDocumentRepository kycDocumentRepository;
    private final ParticulierRepository particulierRepository;
    private final AgenceRepository agenceRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── SUBMIT DOCUMENT ──────────────────────────────────────────────────────

    @Transactional
    public KycDocument submitDocument(Long userId, String userType, String documentType,
                                      MultipartFile file, String userEmail) throws IOException {
        // 1. Validate MIME type (not just extension)
        String detectedMime = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
            throw new BusinessException("Type de fichier non autorisé: " + detectedMime + ". Autorisés: JPEG, PNG, PDF");
        }

        // 2. Validate size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Fichier trop volumineux. Maximum: 5MB");
        }

        // 3. Compute SHA-256 hash
        byte[] bytes = file.getBytes();
        String hash = computeSha256(bytes);

        // 4. Check duplicate (same file already submitted)
        if (kycDocumentRepository.findByFileHash(hash).isPresent()) {
            throw new BusinessException("Ce fichier a déjà été soumis");
        }

        // 5. Store file outside webroot
        Path storagePath = Paths.get(STORAGE_DIR, userType.toLowerCase(), userId.toString());
        Files.createDirectories(storagePath);
        String safeFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        Path filePath = storagePath.resolve(safeFileName);
        Files.write(filePath, bytes);

        // 6. Build and save entity
        KycDocument doc = new KycDocument();
        doc.setUserId(userId);
        doc.setUserType(userType.toUpperCase());
        doc.setDocumentType(documentType.toUpperCase());
        doc.setFileName(safeFileName);
        doc.setFilePath(filePath.toString());
        doc.setFileHash(hash);
        doc.setMimeType(detectedMime);
        doc.setFileSize(file.getSize());
        doc.setKycState(KycDocumentState.SUBMITTED);
        doc.setSubmittedAt(LocalDateTime.now());

        KycDocument saved = kycDocumentRepository.save(doc);
        log.info("KYC document submitted — id={}, userId={}, type={}", saved.getId(), userId, documentType);

        // 7. Publish event (ING5 listens for email)
        eventPublisher.publishEvent(new KycSubmittedEvent(this, saved, userEmail));

        return saved;
    }

    // ── ADMIN: VERIFY DOCUMENT ───────────────────────────────────────────────

    @Transactional
    public KycDocument verifyDocument(Long documentId, String adminEmail, String userEmail) {
        KycDocument doc = findDocumentOrThrow(documentId);

        if (doc.getKycState() != KycDocumentState.SUBMITTED) {
            throw new BusinessException("Document non en attente de vérification. État actuel: " + doc.getKycState());
        }

        doc.setKycState(KycDocumentState.VERIFIED);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewedBy(adminEmail);

        KycDocument saved = kycDocumentRepository.save(doc);
        log.info("KYC document verified — id={}, by={}", documentId, adminEmail);

        // Update user's KycStatus to VERIFIED
        updateUserKycStatus(doc.getUserId(), doc.getUserType(), KycStatus.VERIFIED);

        eventPublisher.publishEvent(new KycReviewedEvent(this, saved, userEmail, KycDocumentState.VERIFIED));
        return saved;
    }

    // ── ADMIN: REJECT DOCUMENT ───────────────────────────────────────────────

    @Transactional
    public KycDocument rejectDocument(Long documentId, String reason, String adminEmail, String userEmail) {
        KycDocument doc = findDocumentOrThrow(documentId);

        if (doc.getKycState() != KycDocumentState.SUBMITTED) {
            throw new BusinessException("Document non en attente de vérification. État actuel: " + doc.getKycState());
        }

        doc.setKycState(KycDocumentState.REJECTED);
        doc.setRejectionReason(reason);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewedBy(adminEmail);

        KycDocument saved = kycDocumentRepository.save(doc);
        log.info("KYC document rejected — id={}, reason={}", documentId, reason);

        updateUserKycStatus(doc.getUserId(), doc.getUserType(), KycStatus.REJECTED);

        eventPublisher.publishEvent(new KycReviewedEvent(this, saved, userEmail, KycDocumentState.REJECTED));
        return saved;
    }

    // ── READ: GET DOCUMENT WITH INTEGRITY CHECK ──────────────────────────────

    @Transactional(readOnly = true)
    public KycDocument getDocumentWithIntegrityCheck(Long documentId) throws IOException {
        KycDocument doc = findDocumentOrThrow(documentId);

        // Re-read file and verify hash matches stored hash
        Path filePath = Paths.get(doc.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException("Fichier physique introuvable pour le document id=" + documentId);
        }

        byte[] fileBytes = Files.readAllBytes(filePath);
        String currentHash = computeSha256(fileBytes);

        if (!currentHash.equals(doc.getFileHash())) {
            log.error("INTEGRITY VIOLATION — document id={}, stored={}, computed={}", documentId, doc.getFileHash(), currentHash);
            throw new BusinessException("Intégrité du fichier compromise — document id=" + documentId);
        }

        return doc;
    }

    // ── LIST ─────────────────────────────────────────────────────────────────

    public List<KycDocument> getDocumentsByUser(Long userId, String userType) {
        return kycDocumentRepository.findByUserIdAndUserType(userId, userType.toUpperCase());
    }

    public List<KycDocument> getPendingDocuments() {
        return kycDocumentRepository.findByKycState(KycDocumentState.SUBMITTED);
    }

    // ── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private KycDocument findDocumentOrThrow(Long id) {
        return kycDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Document KYC introuvable: " + id));
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("Erreur calcul SHA-256", e);
        }
    }

    private String detectMimeType(MultipartFile file) throws IOException {
        // Use actual bytes, not just declared content-type
        byte[] header = file.getBytes();
        if (header.length >= 4) {
            // JPEG: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF)
                return "image/jpeg";
            // PNG: 89 50 4E 47
            if ((header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G')
                return "image/png";
            // PDF: 25 50 44 46
            if (header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F')
                return "application/pdf";
        }
        // Fallback to declared type
        String declared = file.getContentType();
        return declared != null ? declared : "application/octet-stream";
    }

    private void updateUserKycStatus(Long userId, String userType, KycStatus status) {
        switch (userType) {
            case "PARTICULIER" -> particulierRepository.findById(userId).ifPresent(u -> {
                u.setKycStatus(status); particulierRepository.save(u);
            });
            case "AGENCE" -> agenceRepository.findById(userId).ifPresent(u -> {
                u.setKycStatus(status); agenceRepository.save(u);
            });
            case "ENTREPRISE" -> entrepriseRepository.findById(userId).ifPresent(u -> {
                u.setKycStatus(status); entrepriseRepository.save(u);
            });
            default -> log.warn("Unknown userType for KYC status update: {}", userType);
        }
    }
}