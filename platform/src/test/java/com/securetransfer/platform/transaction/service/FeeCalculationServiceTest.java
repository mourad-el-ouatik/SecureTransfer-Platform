package com.securetransfer.platform.transaction.service;

import com.securetransfer.platform.user.repository.AgenceRepository;
import com.securetransfer.platform.user.repository.EntrepriseRepository;
import com.securetransfer.platform.user.repository.ParticulierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeCalculationServiceTest {

    @Mock
    private ParticulierRepository particulierRepository;

    @Mock
    private AgenceRepository agenceRepository;

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @InjectMocks
    private FeeCalculationService feeCalculationService;

    @Test
    void calculateFeeForParticulier_shouldApply1_5Percent() {
        when(particulierRepository.existsById(1L)).thenReturn(true);

        BigDecimal fee = feeCalculationService.calculateFee(1L, new BigDecimal("1000"));

        assertEquals(new BigDecimal("15.00"), fee);
    }

    @Test
    void calculateFeeForAgence_shouldApply0_8Percent() {
        when(particulierRepository.existsById(2L)).thenReturn(false);
        when(agenceRepository.existsById(2L)).thenReturn(true);

        BigDecimal fee = feeCalculationService.calculateFee(2L, new BigDecimal("1000"));

        assertEquals(new BigDecimal("8.00"), fee);
    }

    @Test
    void calculateFeeForEntreprise_shouldApply0_5Percent() {
        when(particulierRepository.existsById(3L)).thenReturn(false);
        when(agenceRepository.existsById(3L)).thenReturn(false);
        when(entrepriseRepository.existsById(3L)).thenReturn(true);

        BigDecimal fee = feeCalculationService.calculateFee(3L, new BigDecimal("1000"));

        assertEquals(new BigDecimal("5.00"), fee);
    }
}
