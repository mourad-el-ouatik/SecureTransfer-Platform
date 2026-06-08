// RoleRepository.java — dans user/repository/
package com.securetransfer.platform.user.repository;

import com.securetransfer.platform.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
