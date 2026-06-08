// UserController.java — dans user/controller/
package com.securetransfer.platform.user.controller;

import com.securetransfer.platform.user.dto.*;
import com.securetransfer.platform.user.entity.KycStatus;
import com.securetransfer.platform.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── POST /api/users/particuliers — Créer un particulier (public) ─
    @PostMapping("/particuliers")
    public ResponseEntity<UserResponse> createParticulier(
            @Valid @RequestBody CreateParticulierRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createParticulier(request));
    }

    // ── POST /api/users/agences — Créer une agence (admin seulement) ─
    @PostMapping("/agences")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createAgence(
            @Valid @RequestBody CreateAgenceRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createAgence(request));
    }

    // ── GET /api/users/{id} — Voir un profil (admin OU propriétaire) ─
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    // ── GET /api/users/me — Mon propre profil (connecté) ────────────
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getUserByEmail(principal.getUsername()));
    }

    // ── GET /api/users — Liste paginée (admin seulement) ────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAll(
            @RequestParam(required = false) KycStatus kycStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.getAllUsers(kycStatus, page, size));
    }

    // ── PATCH /api/users/{id}/kyc — Mettre à jour KYC (admin) ────
    @PatchMapping("/{id}/kyc")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateKyc(
            @PathVariable Long id,
            @RequestParam KycStatus status) {
        userService.updateKycStatus(id, status);
        return ResponseEntity.noContent().build();
    }
}
