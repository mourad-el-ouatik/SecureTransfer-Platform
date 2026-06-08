# Module ING2 — Gestion des Utilisateurs
**Projet SecureTransfer **

---

## Informations générales

| Champ | Valeur |
|-------|--------|
| Module | ING2 — User Management |
| Dépend de | ING1 (Auth, JWT, SecurityConfig) |
| Expose vers | ING3 (Transferts), ING4 (KYC), ING6 (Notifications) |
| Dernière mise à jour | Mai 2026 |

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Ce qui a été implémenté](#2-ce-qui-a-été-implémenté)
3. [Architecture du module](#3-architecture-du-module)
4. [Couche Entités](#4-couche-entités)
5. [Couche DTOs](#5-couche-dtos)
6. [Couche Repositories](#6-couche-repositories)
7. [Mapping avec MapStruct](#7-mapping-avec-mapstruct)
8. [Couche Service](#8-couche-service)
9. [Couche Controller — API REST](#9-couche-controller--api-rest)
10. [Sécurité et chiffrement](#10-sécurité-et-chiffrement)
11. [Migration SQL (Flyway)](#11-migration-sql-flyway)
12. [Tests](#12-tests)
13. [Ce que les autres modules consomment](#13-ce-que-les-autres-modules-consomment)
14. [Erreurs connues et solutions](#14-erreurs-connues-et-solutions)

---
##  Architecture du module

```
com.securetransfer.platform/
│
├── [ING1 — NE PAS MODIFIER]
│   ├── config/          ← JwtAuthFilter, SecurityConfig (modifié légèrement)
│   ├── controller/      ← AuthController
│   ├── dto/             ← LoginRequest, RegisterRequest, AuthResponse
│   ├── entity/          ← UserCredential
│   ├── repository/      ← UserRepository
│   ├── security/        ← JwtService, MfaService
│   └── service/         ← AuthService, UserDetailsServiceImpl
│
├── [ING2 — VOTRE MODULE]
│   ├── user/
│   │   ├── entity/      ← BaseUser, Particulier, Agence, Entreprise, Role, Permission, KycStatus
│   │   ├── repository/  ← ParticulierRepository, AgenceRepository, EntrepriseRepository, RoleRepository
│   │   ├── dto/         ← CreateParticulierRequest, CreateAgenceRequest, CreateEntrepriseRequest,
│   │   │                   UpdateUserRequest, UserResponse
│   │   ├── mapper/      ← UserMapper (MapStruct)
│   │   ├── service/     ← UserService
│   │   └── controller/  ← UserController
│   │
│   └── common/
│       ├── util/        ← EncryptedStringConverter
│       └── exception/   ← BusinessException, ResourceNotFoundException
│
└── resources/
    └── db/migration/
        └── V2__users.sql   ← Migration Flyway ING2
```

---
## 1. Vue d'ensemble

Le module ING2 est responsable de la gestion complète du cycle de vie des utilisateurs de la plateforme SecureTransfer. Il s'appuie sur le module ING1 pour l'authentification et la sécurité, et expose ses services aux modules ING3, ING4 et ING6.

Trois types de profils utilisateurs sont supportés, chacun avec ses propres attributs métier :
- **Particulier** : personne physique, identifiée par son CIN et sa date de naissance
- **Agence** : structure disposant d'un code agence unique et d'une adresse
- **Entreprise** : personne morale identifiée par son SIRET et sa raison sociale

Tous partagent des attributs communs : email, téléphone, statut KYC, limites de transaction, rôles, et timestamps d'audit.

---

## 2. Ce qui a été implémenté

### Gestion des profils
- Création de comptes Particulier, Agence et Entreprise avec validation des champs à l'entrée
- Récupération d'un profil par identifiant ou par email
- Mise à jour des informations de contact
- Listing paginé des utilisateurs avec filtre optionnel par statut KYC

### Gestion des rôles et permissions
- Quatre rôles initiaux insérés en base : `ROLE_USER`, `ROLE_ADMIN`, `ROLE_AGENCE`, `ROLE_ENTREPRISE`
- Attribution automatique de `ROLE_USER` à chaque nouveau Particulier
- Système de permissions granulaires (ex : `TRANSFER_CREATE`, `KYC_UPDATE`) associées aux rôles via une table de jointure

### Statut KYC
- Chaque utilisateur démarre avec le statut `PENDING`
- L'administrateur peut passer le statut à `VERIFIED` ou `REJECTED` via un endpoint dédié
- Le statut KYC est exposé aux autres modules via `UserResponse`

### Limites de transaction
- Chaque profil Particulier est créé avec des limites par défaut (10 000 quotidien / 2 000 unitaire)
- Une méthode de validation des limites est exposée au module ING3 avant tout transfert

### Sécurité des données sensibles
- Un convertisseur JPA chiffre et déchiffre automatiquement les champs sensibles en base via AES-256
- La clé de chiffrement est configurée dans `application.yml` et ne doit jamais être versionnée

---

## 3. Architecture du module

Le module est organisé en deux branches de packages sous `com.securetransfer.platform` :

### Branche `user/`
Contient toute la logique métier liée aux utilisateurs, divisée en six couches : entité, repository, DTO, mapper, service et controller.

### Branche `common/`
Contient les utilitaires transversaux partagés par le module :
- `EncryptedStringConverter` : chiffrement AES-256 transparent pour JPA
- `BusinessException` : exception métier avec message explicite
- `ResourceNotFoundException` : exception pour les ressources introuvables (retourne HTTP 404)

### Ressources
Le fichier de migration SQL `V2__users.sql` est placé dans `src/main/resources/db/migration/` et est appliqué automatiquement par Flyway au démarrage.

---

## 4. Couche Entités

### `KycStatus` (Enum)
Représente les trois états possibles du processus de vérification d'identité d'un utilisateur : `PENDING`, `VERIFIED`, `REJECTED`.

### `Permission`
Entité JPA représentant une permission fonctionnelle (ex : `TRANSFER_CREATE`). Stockée dans la table `permissions`.

### `Role`
Entité JPA représentant un rôle utilisateur (ex : `ROLE_ADMIN`). Un rôle possède un ensemble de permissions, chargé eagerly. Stocké dans la table `roles` avec une table de jointure `role_permissions`.

### `BaseUser` (classe abstraite)
Classe mère annotée `@MappedSuperclass` dont héritent les trois types de profils. Elle centralise tous les champs communs :
- Identifiant auto-généré
- Email (unique, non null)
- Numéro de téléphone
- Statut KYC (valeur par défaut : `PENDING`)
- Limites de transaction quotidienne et unitaire
- Ensemble de rôles (relation `@ManyToMany` chargée eagerly)
- Champ `@Version` pour la gestion optimiste de la concurrence
- Timestamps `createdAt` et `updatedAt` gérés automatiquement via `@EnableJpaAuditing`

### `Particulier`
Hérite de `BaseUser`. Ajoute le CIN, la date de naissance et la nationalité. Persisté dans la table `particuliers`.

### `Agence`
Hérite de `BaseUser`. Ajoute le nom de l'agence, un code agence unique et une adresse. Persistée dans la table `agences`.

### `Entreprise`
Hérite de `BaseUser`. Ajoute la raison sociale, le SIRET (unique) et une adresse. Persistée dans la table `entreprises`.

---

## 5. Couche DTOs

Les DTOs sont implémentés sous forme de **records Java** (immuables, sans boilerplate).

### Requêtes de création
Trois records distincts couvrent la création de chaque type de profil : `CreateParticulierRequest`, `CreateAgenceRequest` et `CreateEntrepriseRequest`. Ils embarquent des contraintes de validation Bean Validation (`@NotBlank`, `@Email`, `@Size`) appliquées automatiquement par Spring avant d'atteindre la couche service.

### `UpdateUserRequest`
Permet la mise à jour partielle d'un profil : numéro de téléphone et adresse uniquement.

### `UserResponse`
Objet de réponse universel retourné par tous les endpoints et méthodes de service. Il expose uniquement les données nécessaires aux consommateurs : `id`, `email`, `kycStatus`, `dailyTransactionLimit`, `singleTransactionLimit` et `createdAt`. Les données internes (mot de passe hashé, version JPA, etc.) ne sont jamais exposées.

---

## 6. Couche Repositories

Quatre interfaces Spring Data JPA ont été créées :

- `ParticulierRepository`, `AgenceRepository`, `EntrepriseRepository` : étendent `JpaRepository` et exposent chacun une méthode `findByEmail`, un `existsByEmail` pour éviter les doublons, et une méthode paginée `findAllFiltered` qui accepte un filtre optionnel par statut KYC via une requête JPQL.
- `RoleRepository` : permet de retrouver un rôle par son nom (ex : `findByName("ROLE_USER")`).

---

## 7. Mapping avec MapStruct

`UserMapper` est une interface annotée `@Mapper(componentModel = "spring")` dont l'implémentation est générée automatiquement à la compilation par MapStruct.

Elle gère deux directions de conversion :
- **Entité → DTO** : trois surcharges de `toResponse()` pour Particulier, Agence et Entreprise, toutes retournant un `UserResponse`
- **DTO → Entité** : `toParticulier()`, `toAgence()`, `toEntreprise()` à partir des requêtes de création
- **Mise à jour partielle** : `updateParticulierFromRequest()` avec `@MappingTarget` pour modifier en place une entité existante sans la recréer

MapStruct élimine tout le code de mapping manuel répétitif. L'implémentation générée `UserMapperImpl` est visible dans `target/generated-sources/` après un build.

---

## 8. Couche Service

`UserService` est le point d'entrée unique pour toute la logique métier du module. Les autres modules interagissent exclusivement avec lui, sans jamais accéder directement aux repositories.

### Création d'utilisateurs
Lors de la création d'un Particulier, le service vérifie l'absence de doublon email, encode le mot de passe via `PasswordEncoder`, attribue le rôle `ROLE_USER`, définit les limites de transaction par défaut, puis persiste l'entité et retourne un `UserResponse`.

Le même principe s'applique pour les Agences et Entreprises, avec les rôles correspondants.

### Récupération de profils
Le service expose `getUser(Long id)` et `getUserByEmail(String email)`, qui lèvent une `ResourceNotFoundException` si l'utilisateur n'existe pas.

### Listing paginé
`getAllUsers(KycStatus filter, int page, int size)` retourne une page de `UserResponse` avec filtre optionnel sur le statut KYC.

### Validation des limites (pour ING3)
`validateTransactionLimit(Long userId, BigDecimal amount)` récupère l'utilisateur concerné et lève une `BusinessException` si le montant demandé dépasse sa limite unitaire.

### Mise à jour du statut KYC (pour ING4)
`updateKycStatus(Long userId, KycStatus newStatus)` met à jour le statut KYC de n'importe quel type d'utilisateur.

---

## 9. Couche Controller — API REST

`UserController` expose six endpoints REST sous le préfixe `/api/users`.

| Méthode | URL | Auth requise | Description |
|---------|-----|-------------|-------------|
| `POST` | `/api/users/particuliers` | Aucune | Crée un compte Particulier |
| `POST` | `/api/users/agences` | `ROLE_ADMIN` | Crée un compte Agence |
| `GET` | `/api/users/me` | JWT valide | Retourne le profil de l'utilisateur connecté |
| `GET` | `/api/users/{id}` | ADMIN ou propriétaire | Retourne un profil par ID |
| `GET` | `/api/users` | `ROLE_ADMIN` | Liste paginée avec filtre KYC optionnel |
| `PATCH` | `/api/users/{id}/kyc` | `ROLE_ADMIN` | Met à jour le statut KYC |

L'endpoint `GET /me` utilise `@AuthenticationPrincipal` pour extraire l'email depuis le contexte de sécurité Spring et délègue à `getUserByEmail()`.

L'endpoint `GET /{id}` utilise `@PreAuthorize` avec une expression SpEL qui autorise l'accès soit aux administrateurs, soit au propriétaire du profil via un bean `userSecurity`.

---

## 10. Sécurité et chiffrement

### Modification de SecurityConfig (ING1)
La seule modification apportée au module ING1 est l'ajout de l'endpoint `/api/users/particuliers` dans la liste des URLs publiques. Cela permet l'auto-inscription des particuliers sans token JWT. Tous les autres endpoints du module ING2 restent protégés.

### Chiffrement AES-256
`EncryptedStringConverter` implémente `AttributeConverter<String, String>` de JPA. Il intercepte automatiquement les lectures et écritures en base pour les champs annotés avec ce converter. Le chiffrement utilise AES avec une clé de 32 caractères configurée dans `application.yml` sous la clé `encryption.key`. Cette clé doit être au niveau racine du fichier YAML, au même niveau que `jwt:`.

---

## 11. Migration SQL (Flyway)

Le fichier `V2__users.sql` est appliqué automatiquement par Flyway au démarrage, après la migration V1 d'ING1.

Il crée les tables suivantes :
- `roles` et `permissions` avec leur table de jointure `role_permissions`
- `particuliers`, `agences` et `entreprises` avec tous leurs champs métier et d'audit
- `user_roles` : table de jointure polymorphe reliant les utilisateurs à leurs rôles

Il insère également les quatre rôles initiaux (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_AGENCE`, `ROLE_ENTREPRISE`) avec `ON CONFLICT DO NOTHING` pour rendre la migration idempotente.

Au démarrage de l'application, les logs Flyway confirment l'application avec le message :
```
Successfully applied 1 migration to schema "public", now at version v2
```

---

## 12. Tests

Les tests unitaires se trouvent dans `UserServiceTest.java` et couvrent les cas critiques du service avec Mockito (sans base de données réelle).

| Test | Résultat |
|------|----------|
| Création d'un Particulier avec données valides → retourne `UserResponse` | ✅ |
| Création avec email déjà existant → lève `BusinessException` | ✅ |
| Validation de transaction avec montant supérieur à la limite → lève `BusinessException` | ✅ |

Les trois tests passent en moins de 2 secondes.

---

## 13. Ce que les autres modules consomment

Les modules ING3, ING4 et ING6 injectent uniquement `UserService`. Ils n'ont pas accès aux repositories ni aux entités internes.

### ING3 — Transferts
Avant d'exécuter un transfert, ING3 appelle `validateTransactionLimit(userId, montant)`. Si le montant dépasse la limite unitaire de l'utilisateur, une `BusinessException` est levée et le transfert est bloqué.

### ING4 — KYC
Après avoir validé l'identité d'un utilisateur, ING4 appelle `updateKycStatus(userId, KycStatus.VERIFIED)` ou `updateKycStatus(userId, KycStatus.REJECTED)`.

### ING3, ING4, ING6 — Récupération de profils
Les trois modules peuvent récupérer un profil via `getUser(id)`, `getUserByEmail(email)`, ou une liste paginée via `getAllUsers(filter, page, size)`. Tous retournent des objets `UserResponse`.

### Règles de collaboration inter-modules
- Ne jamais injecter directement `ParticulierRepository`, `AgenceRepository` ou `EntrepriseRepository` depuis un autre module.
- Ne jamais modifier les classes du module ING1 sans concertation avec l'équipe ING1.
- Toujours communiquer via `UserService` — les entités internes ne sont pas une API publique.

---

## 14. Erreurs connues et solutions

| Erreur | Cause | Solution |
|--------|-------|----------|
| `Could not resolve placeholder 'encryption.key'` | `encryption:` mal indenté dans `application.yml` | Placer `encryption:` au niveau racine, pas sous `jwt:` |
| `package org.mapstruct does not exist` | MapStruct processor absent du `pom.xml` | Ajouter `mapstruct-processor` dans `<annotationProcessorPaths>` |
| `HTTP 403` sur `/api/v1/auth/login` | L'email n'existe pas dans `user_credentials` | Appeler d'abord `/api/v1/auth/register` |
| `HTTP 403` sur `/api/users/me` | L'email du token JWT n'existe pas dans les tables utilisateurs ING2 | Utiliser le même email pour le register ING1 et la création du profil ING2 |
| `HTTP 403` sur `/api/v1/auth/register` | Email déjà utilisé → exception interceptée par Spring Security | Utiliser un email différent |
| `cannot find symbol method createAgence` | Méthode absente dans `UserService` | Implémenter `createAgence()` et `createEntreprise()` dans le service |
| `Build failed: Annotation Processing` | IntelliJ ne traite pas les annotations MapStruct | Activer via File → Settings → Compiler → Annotation Processors |

---
## ✅ Checklist de validation complète

- [ ] Docker : `docker ps` → 2 conteneurs Up (postgres + redis)
- [ ] `pom.xml` : MapStruct ajouté + processor configuré
- [ ] `PlatformApplication.java` : `@EnableJpaAuditing` présent
- [ ] Packages créés : `user/`, `common/`
- [ ] `BusinessException.java` et `ResourceNotFoundException.java` créés
- [ ] `KycStatus.java` créé
- [ ] `Role.java` et `Permission.java` créés avec `@Entity`
- [ ] `BaseUser.java` créé avec `@MappedSuperclass`
- [ ] `Particulier.java`, `Agence.java`, `Entreprise.java` créés
- [ ] `EncryptedStringConverter.java` créé
- [ ] `V2__users.sql` créé → logs Flyway : "2 migrations applied"
- [ ] `application.yml` : `encryption.key` ajouté au niveau racine
- [ ] `RoleRepository`, `ParticulierRepository`, `AgenceRepository`, `EntrepriseRepository` créés
- [ ] DTOs créés : `CreateParticulierRequest`, `CreateAgenceRequest`, `CreateEntrepriseRequest`, `UpdateUserRequest`, `UserResponse`
- [ ] `UserMapper.java` créé → Rebuild → `target/generated-sources/` contient `UserMapperImpl`
- [ ] `UserService.java` créé avec toutes les méthodes
- [ ] `UserController.java` créé avec tous les endpoints
- [ ] `SecurityConfig.java` modifié : `/api/users/particuliers` en public
- [ ] Tests : `mvn test` → 3 tests verts ✅
- [ ] `POST /api/users/particuliers` → HTTP 201 ✅
- [ ] `GET /api/users/me` avec token JWT → HTTP 200 ✅
- [ ] `git push origin feature/ing2-users` ✅
- [ ] 
<img width="1004" height="314" alt="image" src="https://github.com/user-attachments/assets/0a100209-d9a4-449c-baf1-c6d2de07a11c" />

<img width="1004" height="443" alt="image" src="https://github.com/user-attachments/assets/875b8782-4e5c-48e9-a294-f9cbf03a1b41" />
<img width="1004" height="296" alt="image" src="https://github.com/user-attachments/assets/624c415d-45d0-48fa-8d95-8cfc8271f074" />

