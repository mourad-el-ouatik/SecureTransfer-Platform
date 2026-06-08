// UserMapper.java — dans user/mapper/
package com.securetransfer.platform.user.mapper;
import com.securetransfer.platform.user.entity.Entreprise;
import com.securetransfer.platform.user.dto.*;
import com.securetransfer.platform.user.entity.Agence;
import com.securetransfer.platform.user.entity.Particulier;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

// componentModel = "spring" → MapStruct crée un Bean Spring injectable
@Mapper(componentModel = "spring")
public interface UserMapper {

    // Entité → DTO de réponse
    UserResponse toResponse(Particulier particulier);
    UserResponse toResponse(Agence agence);
    UserResponse toResponse(Entreprise entreprise);  // ← AJOUTER

    // DTO de création → Entité
    // ATTENTION : le password ne doit PAS être mappé ici (il sera hashé par le service)
    Particulier toParticulier(CreateParticulierRequest request);
    Agence toAgence(CreateAgenceRequest request);
    Entreprise toEntreprise(CreateEntrepriseRequest request);  // ← AJOUTER


    // Mise à jour : modifier l'entité EXISTANTE (pas en créer une nouvelle)
    void updateParticulierFromRequest(UpdateUserRequest request,
                                      @MappingTarget Particulier target);
}
