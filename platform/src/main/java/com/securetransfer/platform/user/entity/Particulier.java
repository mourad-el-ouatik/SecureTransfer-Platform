// Particulier.java
package com.securetransfer.platform.user.entity;

import com.securetransfer.platform.common.util.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Entity
@Table(name = "particuliers")
@Data
@EqualsAndHashCode(callSuper = true)
public class Particulier extends BaseUser {

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String cin; // CIN chiffré en base

    private String nationality;
    private LocalDate dateOfBirth;
}
