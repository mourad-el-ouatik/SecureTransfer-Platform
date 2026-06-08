# 🔐 Module Authentification – SecureTransfer Platform (ING1)

## 📌 Objectif du module

Ce module est le **socle sécurisé** du projet SecureTransfer. Il fournit :

| Fonctionnalité | Description |
|----------------|-------------|
| **Inscription / Connexion** | Gestion complète des utilisateurs |
| **Authentification JWT** | JSON Web Token (stateless) |
| **Double authentification (MFA)** | Via Google Authenticator (TOTP) |
| **Gestion des sessions** | Stateless avec blacklist Redis |
| **Protection des endpoints** | Via Spring Security |

---

## 🧠 Concepts clés abordés

| Concept | Description |
|---------|-------------|
| **JWT** | Token signé contenant les infos utilisateur, valable 15 min |
| **Refresh Token** | Token longue durée (7 jours) pour renouveler l'access token |
| **BCrypt** | Algorithme de hashage des mots de passe |
| **TOTP** | Time-based One-Time Password (code à 6 chiffres toutes les 30s) |
| **Redis** | Stockage en mémoire pour la blacklist des tokens déconnectés |
| **Flyway** | Migrations SQL automatiques au démarrage |
| **Spring Security** | Filtre de sécurité + gestion des autorisations |

---

## 🏗️ Architecture du module

```
┌─────────────────────────────────────────────────────────────────────┐
│                        REQUÊTE HTTP                                 │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      JwtAuthFilter (filtre)                         │
│  • Extrait le JWT du header Authorization                           │
│  • Vérifie validité (signature + expiration + blacklist Redis)      │
│  • Authentifie l'utilisateur dans Spring Security                   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       AuthController                                │
│  • Reçoit les requêtes HTTP                                         │
│  • Valide les DTOs (@Valid)                                         │
│  • Appelle AuthService                                              │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        AuthService                                  │
│  • Logique métier (register, login, MFA, logout)                    │
│  • Utilise UserRepository, JwtService, MfaService                  │
└──────────┬───────────────────┴───────────────────┬─────────────────┘
           │                                       │
           ▼                   ▼                   ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ UserRepository   │  │   JwtService     │  │   MfaService     │
│ (PostgreSQL)     │  │ (génération JWT) │  │ (TOTP + QR code) │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## 📁 Structure des fichiers

```
src/main/java/.../
├── controller/
│   └── AuthController.java         # Endpoints REST (register, login, MFA...)
├── service/
│   ├── AuthService.java            # Logique métier principale
│   ├── JwtService.java             # Génération + validation JWT
│   ├── MfaService.java             # TOTP + QR code Google Authenticator
│   └── UserDetailsServiceImpl.java # Pont Spring Security ↔ base de données
├── filter/
│   └── JwtAuthFilter.java          # Intercepte chaque requête HTTP
├── config/
│   └── SecurityConfig.java         # Configuration Spring Security
├── entity/
│   └── UserCredential.java         # Entité JPA (table user_credentials)
├── repository/
│   └── UserRepository.java         # Interface JPA (accès PostgreSQL)
├── dto/
│   ├── LoginRequest.java           # email + password + mfaCode (optionnel)
│   ├── RegisterRequest.java        # email + password
│   ├── AuthResponse.java           # token + email + mfaEnabled
│   └── MfaActivationResponse.java  # secret + qrCode (Base64)
└── PlatformApplication.java        # Point d'entrée (main)

src/main/resources/
├── application.yml                 # Config PostgreSQL, Redis, JWT, Flyway
└── db/migration/
    └── V1__create_user_credentials.sql  # Migration Flyway
```

---

## 🗄️ Modèle de données — `UserCredential.java`

| Champ | Type | Rôle |
|-------|------|------|
| `id` | UUID | Identifiant unique |
| `email` | String | Identifiant de connexion (unique) |
| `password` | String | Mot de passe hashé (BCrypt) |
| `mfaSecret` | String | Clé secrète TOTP (si MFA activé) |
| `mfaEnabled` | Boolean | MFA activé ou non |
| `failedAttempts` | Integer | Compteur d'échecs de connexion |
| `accountLocked` | Boolean | Compte bloqué après 5 échecs |
| `lockTime` | LocalDateTime | Date du blocage |
| `createdAt` / `updatedAt` | LocalDateTime | Dates automatiques |

---

## 🌐 Endpoints REST

| Endpoint | Méthode | Rôle | Auth requise |
|----------|---------|------|-------------|
| `/register` | POST | Inscription | ❌ |
| `/login` | POST | Connexion → retourne JWT | ❌ |
| `/logout` | POST | Déconnexion (blacklist JWT) | ✅ |
| `/mfa/activate` | POST | Activer le MFA | ✅ |
| `/mfa/validate` | POST | Valider le MFA | ✅ |
| `/mfa/qrcode` | GET | Obtenir le QR code | ✅ |

---

## 📱 Fonctionnalités

| Fonctionnalité | Statut |
|----------------|--------|
| Inscription avec email + password | ✅ |
| Connexion avec génération JWT | ✅ |
| Hashage BCrypt des mots de passe | ✅ |
| Blacklist Redis à la déconnexion | ✅ |
| MFA via Google Authenticator (TOTP) | ✅ |
| Génération du QR code (ZXing) | ✅ |
| Blocage du compte après 5 échecs | ✅ |
| Migrations SQL automatiques (Flyway) | ✅ |
| Endpoints publics / protégés (Spring Security) | ✅ |

---

## 💻 Extraits de code importants

### Structure d'un JWT généré

```json
{
  "sub": "test@mail.com",
  "userId": "550e8400-...",
  "jti": "abc123-...",
  "iat": 1714800000,
  "exp": 1714800900
}
```

### JwtService — méthodes principales

| Méthode | Rôle |
|---------|------|
| `generateAccessToken()` | Crée un JWT valable **15 min** |
| `generateRefreshToken()` | Crée un JWT valable **7 jours** |
| `extractEmail()` | Récupère l'email depuis un JWT |
| `extractUserId()` | Récupère l'ID depuis un JWT |
| `isTokenValid()` | Vérifie signature + expiration + blacklist |
| `blacklistToken()` | Invalide un JWT dans Redis (déconnexion) |

### Algorithme TOTP (MfaService)

```
Clé secrète (stockée en base ET dans le téléphone)
        ↓
Heure actuelle (divisée par 30 secondes → fenêtre)
        ↓
HMAC-SHA1(clé, fenêtre) → hash
        ↓
Extraction de 4 bytes → conversion en 6 chiffres
```

---

## 🔄 Flux complet — Connexion réussie

```
1. POST /login  {email, password}
        ↓
2. AuthController.login() reçoit la requête
        ↓
3. AuthService.login() vérifie le mot de passe (BCrypt)
        ↓
4. JwtService.generateAccessToken() crée le JWT
        ↓
5. Retourne {token, email} au client
        ↓
6. Le client envoie le JWT dans chaque requête suivante
   Header : Authorization: Bearer <token>
        ↓
7. JwtAuthFilter intercepte chaque requête
   ├── Extrait le JWT
   ├── Vérifie signature + expiration + blacklist Redis
   └── Authentifie l'utilisateur dans SecurityContextHolder
        ↓
8. Le controller accède à l'utilisateur via @AuthenticationPrincipal
```

---

## 🔄 Flux complet — Activation MFA

```
1. POST /mfa/activate?email=xxx
        ↓
2. AuthService.activateMfa()
   ├── MfaService.generateSecret()              → clé secrète (20 bytes)
   ├── MfaService.getGoogleAuthenticatorUrl()   → URL otpauth://...
   ├── MfaService.generateQrCode()              → image PNG (Base64)
   └── Stocke mfaSecret en base (en attente)
        ↓
3. Retourne secret + QR code au client
        ↓
4. L'utilisateur scanne le QR code avec Google Authenticator
        ↓
5. L'utilisateur saisit le code à 6 chiffres
        ↓
6. POST /mfa/validate?email=xxx&code=123456
        ↓
7. AuthService.validateMfa()
   ├── MfaService.verifyCode(secret, code) → vérifie le code TOTP
   └── mfaEnabled = true (activé définitivement)
```

---

## 🛠️ Technologies utilisées

| Technologie | Version | Rôle |
|-------------|---------|------|
| Java | 21 | Langage |
| Spring Boot | 3.5.14 | Framework principal |
| Spring Security | 6.5.10 | Authentification + autorisation |
| Spring Data JPA | - | Accès à PostgreSQL |
| PostgreSQL | 16 | Base de données principale |
| Redis | 7 | Blacklist JWT (tokens déconnectés) |
| JJWT | 0.12.6 | Manipulation des JWT |
| ZXing | 3.5.3 | Génération des QR codes |
| Flyway | 11.7.2 | Migrations SQL automatiques |
| Lombok | - | Getters/setters automatiques |
| Docker | - | Conteneurisation |

---

## 🚀 Commandes utiles

```bash
# Lancer PostgreSQL et Redis
docker compose up -d

# Arrêter les conteneurs
docker compose down

# Voir les conteneurs actifs
docker ps

# Se connecter à PostgreSQL
docker exec -it securetransfer-postgres psql -U root -d securetransfer

# Lancer l'application Spring Boot
./mvnw spring-boot:run
```

---

## 📚 RÉCAPITULATIF — CE QUE CE MODULE FOURNIT

---

### ✅ Ce que les autres modules peuvent utiliser

| Besoin | Solution fournie |
|--------|-----------------|
| Identifier l'utilisateur connecté | `@AuthenticationPrincipal` + JWT |
| Protéger un endpoint par rôle | `@PreAuthorize("hasRole('USER')")` |
| Vérifier qu'un utilisateur existe | `UserRepository.findByEmail()` |
| Obtenir l'ID de l'utilisateur | `jwtService.extractUserId(token)` |
| Stockage temporaire (blacklist, rate limiting) | Redis disponible |

---

### 📝 Points importants pour les autres membres

| Point | Détail |
|-------|--------|
| **Header JWT** | Envoyer `Authorization: Bearer <token>` sur tous les endpoints protégés |
| **Utilisateur connecté** | Accessible via `@AuthenticationPrincipal UserDetails user` |
| **Rôles** | `ROLE_USER` par défaut (à enrichir selon les besoins) |
| **MFA** | Optionnel — l'utilisateur choisit de l'activer |
| **Mots de passe** | Hashés BCrypt — jamais stockés en clair |
| **Redis** | Gère la blacklist des tokens (déconnexion) avec TTL automatique |

---

### 📊 Sécurité — règles appliquées

| Règle | Implémentation |
|-------|----------------|
| Mots de passe hashés | BCrypt (irréversible) |
| Tokens invalidés à la déconnexion | Blacklist Redis avec TTL |
| Sessions stateless | Pas de session HTTP, JWT uniquement |
| Endpoints sensibles protégés | Spring Security (`STATELESS`) |
| CSRF désactivé | Inutile avec JWT (pas de cookies) |
| MFA possible | TOTP via Google Authenticator |
| Blocage après 5 échecs | `accountLocked` + `lockTime` en base |

---

### 👥 Auteur

| Élément | Information |
|---------|-------------|
| **Équipe** | ING1: El Hachimi Abdelhamid — Module Authentification |
| **Projet** | SecureTransfer Platform |

---

### 📅 Version

| Élément | Information |
|---------|-------------|
| **Date** | Mai 2026 |
| **Version** | 1.0 |
| **Statut** | ✅ Finalisé |
