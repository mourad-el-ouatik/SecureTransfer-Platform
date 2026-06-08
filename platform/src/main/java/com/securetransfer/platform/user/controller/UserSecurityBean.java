// UserSecurityBean.java — dans user/controller/
package com.securetransfer.platform.user.controller;

import com.securetransfer.platform.user.repository.ParticulierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurityBean {

    private final ParticulierRepository repo;

    // Retourne true si l'utilisateur connecté est le propriétaire du compte #userId
    public boolean isOwner(Authentication auth, Long userId) {
        String email = auth.getName();
        return repo.findById(userId)
                .map(u -> u.getEmail().equals(email))
                .orElse(false);
    }
}
