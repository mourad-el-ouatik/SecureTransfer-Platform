package com.securetransfer.platform.transaction.service;

import com.securetransfer.platform.user.repository.AgenceRepository;
import com.securetransfer.platform.user.repository.EntrepriseRepository;
import com.securetransfer.platform.user.repository.ParticulierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeCalculationService {

    private final ParticulierRepository particulierRepository;
    private final AgenceRepository agenceRepository;
    private final EntrepriseRepository entrepriseRepository;

    /**
     * Calcul des frais selon le type d'utilisateur
     */
    public BigDecimal calculateFee(Long userId, BigDecimal amount) {
        BigDecimal feeRate;

        if (particulierRepository.existsById(userId)) {
            feeRate = new BigDecimal("0.015"); // 1.5%
        } else if (agenceRepository.existsById(userId)) {
            feeRate = new BigDecimal("0.008"); // 0.8%
        } else if (entrepriseRepository.existsById(userId)) {
            feeRate = new BigDecimal("0.005"); // 0.5%
        } else {
            feeRate = new BigDecimal("0.02"); // 2.0% par défaut
        }

        BigDecimal fee = amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        log.info("Calculated fee for user {}: {} (Rate: {})", userId, fee, feeRate);
        return fee;
    }
}
