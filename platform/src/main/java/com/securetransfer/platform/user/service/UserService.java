// UserService.java — dans user/service/
package com.securetransfer.platform.user.service;

import com.securetransfer.platform.common.exception.BusinessException;
import com.securetransfer.platform.common.exception.ResourceNotFoundException;
import com.securetransfer.platform.user.dto.*;
import com.securetransfer.platform.user.entity.*;
import com.securetransfer.platform.user.mapper.UserMapper;
import com.securetransfer.platform.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final ParticulierRepository particulierRepository;
    private final AgenceRepository agenceRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ── CRÉER UN PARTICULIER ──────────────────────────────────────
    @Transactional
    public UserResponse createParticulier(CreateParticulierRequest request) {
        if (particulierRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email déjà utilisé : " + request.email());
        }

        Particulier user = userMapper.toParticulier(request);
        user.setDailyTransactionLimit(BigDecimal.valueOf(10_000));
        user.setSingleTransactionLimit(BigDecimal.valueOf(2_000));

        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Rôle USER manquant en base"));
        user.getRoles().add(roleUser);

        Particulier saved = particulierRepository.save(user);
        log.info("Particulier créé — id={}, email={}", saved.getId(), saved.getEmail());
        return userMapper.toResponse(saved);
    }

    // ── CRÉER UNE AGENCE ──────────────────────────────────────────
    @Transactional
    public UserResponse createAgence(CreateAgenceRequest request) {
        if (agenceRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email déjà utilisé : " + request.email());
        }

        Agence agence = userMapper.toAgence(request);
        agence.setDailyTransactionLimit(BigDecimal.valueOf(100_000));
        agence.setSingleTransactionLimit(BigDecimal.valueOf(50_000));

        Role roleAgence = roleRepository.findByName("ROLE_AGENCE")
                .orElseThrow(() -> new IllegalStateException("Rôle AGENCE manquant en base"));
        agence.getRoles().add(roleAgence);

        Agence saved = agenceRepository.save(agence);
        log.info("Agence créée — id={}, email={}", saved.getId(), saved.getEmail());
        return userMapper.toResponse(saved);
    }

    // ── CRÉER UNE ENTREPRISE ──────────────────────────────────────
    @Transactional
    public UserResponse createEntreprise(CreateEntrepriseRequest request) {
        if (entrepriseRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email déjà utilisé : " + request.email());
        }

        Entreprise entreprise = userMapper.toEntreprise(request);
        entreprise.setDailyTransactionLimit(BigDecimal.valueOf(500_000));
        entreprise.setSingleTransactionLimit(BigDecimal.valueOf(200_000));

        Role roleEntreprise = roleRepository.findByName("ROLE_ENTREPRISE")
                .orElseThrow(() -> new IllegalStateException("Rôle ENTREPRISE manquant en base"));
        entreprise.getRoles().add(roleEntreprise);

        Entreprise saved = entrepriseRepository.save(entreprise);
        log.info("Entreprise créée — id={}, email={}", saved.getId(), saved.getEmail());
        return userMapper.toResponse(saved);
    }

    // ── RÉCUPÉRER UN UTILISATEUR PAR ID ──────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        Particulier user = particulierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable : " + id));
        return userMapper.toResponse(user);
    }

    // ── RÉCUPÉRER UN UTILISATEUR PAR EMAIL ───────────────────────
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        Particulier user = particulierRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable : " + email));
        return userMapper.toResponse(user);
    }

    // ── LISTE PAGINÉE AVEC FILTRE KYC ─────────────────────────────
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(KycStatus filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return particulierRepository.findAllFiltered(filter, pageable)
                .map(userMapper::toResponse);
    }

    // ── VALIDER LA LIMITE (appelé par ING3 — Transferts) ─────────
    @Transactional(readOnly = true)
    public void validateTransactionLimit(Long userId, BigDecimal amount) {
        Particulier user = particulierRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User : " + userId));
        if (amount.compareTo(user.getSingleTransactionLimit()) > 0) {
            throw new BusinessException(
                    "Montant " + amount + " dépasse la limite unitaire de " + user.getSingleTransactionLimit());
        }
    }

    // ── METTRE À JOUR LE STATUT KYC (appelé par ING4) ────────────
    @Transactional
    public void updateKycStatus(Long userId, KycStatus newStatus) {
        Particulier user = particulierRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User : " + userId));
        user.setKycStatus(newStatus);
        particulierRepository.save(user);
        log.info("KYC mis à jour — userId={}, status={}", userId, newStatus);
    }
}