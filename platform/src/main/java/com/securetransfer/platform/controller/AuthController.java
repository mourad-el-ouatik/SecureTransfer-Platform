package com.securetransfer.platform.controller;

import com.securetransfer.platform.dto.*;
import com.securetransfer.platform.entity.UserCredential;
import com.securetransfer.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. INSCRIPTION
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserCredential user = authService.register(request.getEmail(), request.getPassword());

        AuthResponse response = new AuthResponse();
        response.setEmail(user.getEmail());
        response.setMfaEnabled(false);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. CONNEXION
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(
                request.getEmail(),
                request.getPassword(),
                request.getMfaCode()
        );

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(request.getEmail());

        return ResponseEntity.ok(response);
    }

    // 3. ACTIVATION MFA (génération QR code)
    @PostMapping("/mfa/activate")
    public ResponseEntity<MfaActivationResponse> activateMfa(@RequestParam String email) {
        AuthService.MfaActivationResult result = authService.activateMfa(email);

        // Convertir le QR code en Base64 pour l'envoyer au client
        String qrBase64 = java.util.Base64.getEncoder().encodeToString(result.getQrCode());

        return ResponseEntity.ok(new MfaActivationResponse(result.getSecret(), qrBase64));
    }

    // 4. VALIDATION MFA (après scan du QR code)
    @PostMapping("/mfa/validate")
    public ResponseEntity<Void> validateMfa(@RequestParam String email, @RequestParam String code) {
        authService.validateMfa(email, code);
        return ResponseEntity.ok().build();
    }

    // 5. DÉCONNEXION
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok().build();
    }

    // 6. Générer QR code en image (format PNG)
    @GetMapping("/mfa/qrcode")
    public ResponseEntity<byte[]> getQrCode(@RequestParam String email) {
        AuthService.MfaActivationResult result = authService.activateMfa(email);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(result.getQrCode());
    }
}