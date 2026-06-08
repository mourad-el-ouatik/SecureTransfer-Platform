package com.securetransfer.platform.kyc.controller;

import com.securetransfer.platform.kyc.entity.KycDocument;
import com.securetransfer.platform.kyc.service.KycWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
@Slf4j
public class KycController {

    private final KycWorkflowService kycWorkflowService;

    // ── USER: submit a document ──────────────────────────────────────────────
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'AGENCE', 'ENTREPRISE')")
    public ResponseEntity<KycDocumentResponse> submitDocument(
            @RequestParam Long userId,
            @RequestParam String userType,
            @RequestParam String documentType,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        KycDocument doc = kycWorkflowService.submitDocument(
                userId, userType, documentType, file, userDetails.getUsername());
        return ResponseEntity.ok(toResponse(doc));
    }

    // ── USER: view own documents ─────────────────────────────────────────────
    @GetMapping("/my-documents")
    @PreAuthorize("hasAnyRole('USER', 'AGENCE', 'ENTREPRISE')")
    public ResponseEntity<List<KycDocumentResponse>> getMyDocuments(
            @RequestParam Long userId,
            @RequestParam String userType) {

        return ResponseEntity.ok(
                kycWorkflowService.getDocumentsByUser(userId, userType)
                        .stream().map(this::toResponse).toList()
        );
    }

    // ── ADMIN: list pending documents ────────────────────────────────────────
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<KycDocumentResponse>> getPendingDocuments() {
        return ResponseEntity.ok(
                kycWorkflowService.getPendingDocuments()
                        .stream().map(this::toResponse).toList()
        );
    }

    // ── ADMIN: verify a document ─────────────────────────────────────────────
    @PostMapping("/{documentId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KycDocumentResponse> verifyDocument(
            @PathVariable Long documentId,
            @RequestParam String userEmail,
            @AuthenticationPrincipal UserDetails adminDetails) {

        KycDocument doc = kycWorkflowService.verifyDocument(
                documentId, adminDetails.getUsername(), userEmail);
        return ResponseEntity.ok(toResponse(doc));
    }

    // ── ADMIN: reject a document ─────────────────────────────────────────────
    @PostMapping("/{documentId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KycDocumentResponse> rejectDocument(
            @PathVariable Long documentId,
            @RequestBody Map<String, String> body,
            @RequestParam String userEmail,
            @AuthenticationPrincipal UserDetails adminDetails) {

        String reason = body.getOrDefault("reason", "Documents non conformes");
        KycDocument doc = kycWorkflowService.rejectDocument(
                documentId, reason, adminDetails.getUsername(), userEmail);
        return ResponseEntity.ok(toResponse(doc));
    }

    // ── USER/ADMIN: integrity check ──────────────────────────────────────────
    @GetMapping("/{documentId}/integrity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkIntegrity(
            @PathVariable Long documentId) throws IOException {

        KycDocument doc = kycWorkflowService.getDocumentWithIntegrityCheck(documentId);
        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "integrityOk", true,
                "hash", doc.getFileHash(),
                "state", doc.getKycState()
        ));
    }

    private KycDocumentResponse toResponse(KycDocument doc) {
        return new KycDocumentResponse(
                doc.getId(), doc.getUserId(), doc.getUserType(),
                doc.getDocumentType(), doc.getFileName(),
                doc.getMimeType(), doc.getFileSize(),
                doc.getKycState().name(), doc.getRejectionReason(),
                doc.getSubmittedAt(), doc.getReviewedAt()
        );
    }

    public record KycDocumentResponse(
            Long id, Long userId, String userType, String documentType,
            String fileName, String mimeType, Long fileSize,
            String state, String rejectionReason,
            java.time.LocalDateTime submittedAt, java.time.LocalDateTime reviewedAt
    ) {}
}