// Agence.java
package com.securetransfer.platform.user.entity;

import com.securetransfer.platform.common.util.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Entity
@Table(name = "agences")
@Data
@EqualsAndHashCode(callSuper = true)
public class Agence extends BaseUser {

    @Convert(converter = EncryptedStringConverter.class)
    private String licenseNumber; // Numéro de licence chiffré

    private String city;
    private String address;

    @Column(nullable = false)
    private BigDecimal cashBalance = BigDecimal.ZERO;
}
