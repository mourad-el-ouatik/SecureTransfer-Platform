package com.securetransfer.platform.repository;

import com.securetransfer.platform.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserCredential, UUID> {

    // Trouver un utilisateur par son email
    Optional<UserCredential> findByEmail(String email);

    // Vérifier si un email existe déjà
    boolean existsByEmail(String email);
}