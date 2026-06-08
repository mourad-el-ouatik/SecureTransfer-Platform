package com.securetransfer.platform.kyc;

import com.securetransfer.platform.common.exception.BusinessException;
import com.securetransfer.platform.kyc.entity.KycDocument;
import com.securetransfer.platform.kyc.entity.KycDocumentState;
import com.securetransfer.platform.kyc.repository.KycDocumentRepository;
import com.securetransfer.platform.kyc.service.KycWorkflowService;
import com.securetransfer.platform.user.repository.AgenceRepository;
import com.securetransfer.platform.user.repository.EntrepriseRepository;
import com.securetransfer.platform.user.repository.ParticulierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycWorkflowServiceTest {

    @Mock KycDocumentRepository kycDocumentRepository;
    @Mock ParticulierRepository particulierRepository;
    @Mock AgenceRepository agenceRepository;
    @Mock EntrepriseRepository entrepriseRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks KycWorkflowService kycWorkflowService;

    // ── MIME TYPE VALIDATION ─────────────────────────────────────────────────

    @Test
    void submitDocument_invalidMimeType_throwsBusinessException() {
        // A plain text file — not JPEG/PNG/PDF
        byte[] textBytes = "Hello World".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", textBytes);

        assertThatThrownBy(() ->
                kycWorkflowService.submitDocument(1L, "PARTICULIER", "CIN", file, "user@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Type de fichier non autorisé");
    }

    @Test
    void submitDocument_fileTooLarge_throwsBusinessException() {
        // PDF magic bytes but oversized
        byte[] bigFile = new byte[6 * 1024 * 1024]; // 6MB
        bigFile[0] = '%'; bigFile[1] = 'P'; bigFile[2] = 'D'; bigFile[3] = 'F';
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", bigFile);

        assertThatThrownBy(() ->
                kycWorkflowService.submitDocument(1L, "PARTICULIER", "CIN", file, "user@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("trop volumineux");
    }

    @Test
    void submitDocument_validJpeg_savesDocument() throws IOException {
        // JPEG magic bytes
        byte[] jpegBytes = new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "cin.jpg", "image/jpeg", jpegBytes);

        when(kycDocumentRepository.findByFileHash(any())).thenReturn(Optional.empty());
        when(kycDocumentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        KycDocument result = kycWorkflowService.submitDocument(
                1L, "PARTICULIER", "CIN", file, "user@test.com");

        assertThat(result.getKycState()).isEqualTo(KycDocumentState.SUBMITTED);
        assertThat(result.getFileHash()).isNotBlank().hasSize(64);
        verify(eventPublisher).publishEvent(any());
    }

    // ── STATE MACHINE ────────────────────────────────────────────────────────

    @Test
    void verifyDocument_notSubmitted_throwsBusinessException() {
        KycDocument doc = new KycDocument();
        doc.setId(1L);
        doc.setKycState(KycDocumentState.PENDING); // not SUBMITTED

        when(kycDocumentRepository.findById(1L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() ->
                kycWorkflowService.verifyDocument(1L, "admin@test.com", "user@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("non en attente");
    }

    @Test
    void verifyDocument_submittedDocument_setsVerifiedState() {
        KycDocument doc = new KycDocument();
        doc.setId(1L);
        doc.setUserId(10L);
        doc.setUserType("PARTICULIER");
        doc.setKycState(KycDocumentState.SUBMITTED);

        when(kycDocumentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(kycDocumentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(particulierRepository.findById(10L)).thenReturn(Optional.empty());

        KycDocument result = kycWorkflowService.verifyDocument(1L, "admin@test.com", "user@test.com");

        assertThat(result.getKycState()).isEqualTo(KycDocumentState.VERIFIED);
        assertThat(result.getReviewedBy()).isEqualTo("admin@test.com");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void rejectDocument_submittedDocument_setsRejectedWithReason() {
        KycDocument doc = new KycDocument();
        doc.setId(2L);
        doc.setUserId(20L);
        doc.setUserType("AGENCE");
        doc.setKycState(KycDocumentState.SUBMITTED);

        when(kycDocumentRepository.findById(2L)).thenReturn(Optional.of(doc));
        when(kycDocumentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(agenceRepository.findById(20L)).thenReturn(Optional.empty());

        KycDocument result = kycWorkflowService.rejectDocument(
                2L, "Document illisible", "admin@test.com", "user@test.com");

        assertThat(result.getKycState()).isEqualTo(KycDocumentState.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Document illisible");
    }

    // ── DUPLICATE FILE ───────────────────────────────────────────────────────

    @Test
    void submitDocument_duplicateHash_throwsBusinessException() {
        byte[] pdfBytes = {'%', 'P', 'D', 'F', 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", pdfBytes);

        when(kycDocumentRepository.findByFileHash(any()))
                .thenReturn(Optional.of(new KycDocument())); // already exists

        assertThatThrownBy(() ->
                kycWorkflowService.submitDocument(1L, "PARTICULIER", "PASSPORT", file, "user@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà été soumis");
    }
}