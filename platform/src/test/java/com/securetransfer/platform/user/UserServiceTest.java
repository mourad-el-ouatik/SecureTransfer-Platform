// UserServiceTest.java — dans src/test/java/com/securetransfer/platform/user/
package com.securetransfer.platform.user;

import com.securetransfer.platform.common.exception.BusinessException;
import com.securetransfer.platform.user.dto.CreateParticulierRequest;
import com.securetransfer.platform.user.entity.Particulier;
import com.securetransfer.platform.user.entity.Role;
import com.securetransfer.platform.user.mapper.UserMapper;
import com.securetransfer.platform.user.repository.ParticulierRepository;
import com.securetransfer.platform.user.repository.RoleRepository;
import com.securetransfer.platform.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private ParticulierRepository particulierRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    @Test
    @DisplayName("createParticulier — email déjà existant → BusinessException")
    void createParticulier_emailExists_throwsException() {
        // Arrange : simuler que l'email existe déjà
        given(particulierRepository.existsByEmail("test@test.com")).willReturn(true);

        CreateParticulierRequest req = new CreateParticulierRequest(
                "test@test.com", "password123", "+212600000000",
                "AB123456", LocalDate.of(1995, 1, 1), "Marocain");

        // Act + Assert : la méthode doit lancer BusinessException
        assertThatThrownBy(() -> userService.createParticulier(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email déjà utilisé");

        // Vérifier que save() n'a JAMAIS été appelé
        then(particulierRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("validateTransactionLimit — montant > limite → BusinessException")
    void validateLimit_exceeds_throws() {
        Particulier user = new Particulier();
        user.setSingleTransactionLimit(BigDecimal.valueOf(2_000));
        given(particulierRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() ->
                userService.validateTransactionLimit(1L, BigDecimal.valueOf(5_000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dépasse la limite");
    }

    @Test
    @DisplayName("createParticulier — création réussie → retourne UserResponse")
    void createParticulier_success() {
        // Arrange
        given(particulierRepository.existsByEmail("new@test.com")).willReturn(false);
        Particulier entity = new Particulier();
        given(userMapper.toParticulier(any())).willReturn(entity);
        given(roleRepository.findByName("ROLE_USER")).willReturn(Optional.of(new Role()));
        given(particulierRepository.save(any())).willReturn(entity);
        given(userMapper.toResponse(entity)).willReturn(null); // simplifié

        CreateParticulierRequest req = new CreateParticulierRequest(
                "new@test.com", "password123", "+212600000000",
                "AB999999", LocalDate.of(1990, 6, 15), "Marocain");

        // Act — ne doit pas lancer d'exception
        assertThatNoException().isThrownBy(() -> userService.createParticulier(req));

        // Verify — save() a bien été appelé
        then(particulierRepository).should().save(any());
    }
}
