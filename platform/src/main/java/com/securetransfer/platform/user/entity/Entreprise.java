// Entreprise.java
package com.securetransfer.platform.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "entreprises")
@Data
@EqualsAndHashCode(callSuper = true)
public class Entreprise extends BaseUser {

    private String registrationNumber;
    private String legalName;
}
