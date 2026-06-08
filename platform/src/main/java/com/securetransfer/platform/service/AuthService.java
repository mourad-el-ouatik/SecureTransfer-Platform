package com.securetransfer.platform.service;

import com.securetransfer.platform.entity.UserCredential;
import com.securetransfer.platform.repository.UserRepository;
import com.securetransfer.platform.security.JwtService;
import com.securetransfer.platform.security.MfaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final MfaService mfaService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    // 1. INSCRIPTION
    @Transactional
    public UserCredential register(String email, String password) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Créer le nouvel utilisateur
        UserCredential user = UserCredential.builder()
                .email(email)
                .password(passwordEncoder.encode(password))  // Mot de passe hashé
                .mfaEnabled(false)
                .failedAttempts(0)
                .accountLocked(false)
                .build();

        return userRepository.save(user);
    }

    // 2. CONNEXION (sans MFA ou avec MFA)
    @Transactional
    public String login(String email, String password, String mfaCode) {
        // Trouver l'utilisateur
        UserCredential user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        // Vérifier si le compte est bloqué
        if (user.isAccountLocked()) {
            // Vérifier si le déblocage est automatique (après 15 min)
            if (user.getLockTime() != null &&
                    user.getLockTime().plusSeconds(900).isBefore(Instant.now())) {
                user.setAccountLocked(false);
                user.setFailedAttempts(0);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Compte bloqué. Réessayez plus tard.");
            }
        }

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // Incrémenter les tentatives échouées
            user.setFailedAttempts(user.getFailedAttempts() + 1);

            // Bloquer après 5 échecs
            if (user.getFailedAttempts() >= 5) {
                user.setAccountLocked(true);
                user.setLockTime(Instant.now());
            }
            userRepository.save(user);
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        // Réinitialiser les tentatives échouées (connexion réussie)
        user.setFailedAttempts(0);
        userRepository.save(user);

        // Vérifier MFA si activé
        if (user.isMfaEnabled()) {
            if (mfaCode == null || !mfaService.verifyCode(user.getMfaSecret(), mfaCode)) {
                throw new RuntimeException("Code MFA invalide");
            }
        }

        // Générer le JWT
        return jwtService.generateAccessToken(user.getEmail(), user.getId());
    }

    // 3. ACTIVATION MFA (générer le QR code)
    @Transactional
    public MfaActivationResult activateMfa(String email) {
        UserCredential user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.isMfaEnabled()) {
            throw new RuntimeException("MFA déjà activé");
        }

        // Générer une nouvelle clé secrète
        String secret = mfaService.generateSecret();

        // Générer l'URL pour Google Authenticator
        String url = mfaService.getGoogleAuthenticatorUrl(secret, email, "SecureTransfer");

        // Générer le QR code
        byte[] qrCode = mfaService.generateQrCode(url, 200, 200);

        // Stocker temporairement la clé secrète (en attente de validation)
        user.setMfaSecret(secret);
        userRepository.save(user);

        return new MfaActivationResult(secret, qrCode);
    }

    // 4. VALIDATION MFA (après scan du QR code)
    @Transactional
    public void validateMfa(String email, String code) {
        UserCredential user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.isMfaEnabled()) {
            throw new RuntimeException("MFA déjà activé");
        }

        // Vérifier le code
        if (mfaService.verifyCode(user.getMfaSecret(), code)) {
            user.setMfaEnabled(true);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Code MFA invalide");
        }
    }

    // 5. DÉCONNEXION
    public void logout(String token) {
        jwtService.blacklistToken(token);
    }

    // Classe interne pour le résultat d'activation MFA
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class MfaActivationResult {
        private String secret;
        private byte[] qrCode;
    }
}