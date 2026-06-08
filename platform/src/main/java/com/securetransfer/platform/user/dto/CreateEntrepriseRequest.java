// CreateEntrepriseRequest.java — dans user/dto/
package com.securetransfer.platform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEntrepriseRequest(

        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Mot de passe minimum 8 caractères")
        String password,

        @NotBlank(message = "Le numéro SIRET est obligatoire")
        String siret,

        String telephone,

        String adresse
) {}