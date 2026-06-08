# SecureTransfer Platform

A secure financial transfer platform built with **Spring Boot 3 / Java 21**, featuring JWT authentication, MFA, KYC document management, transaction processing with fraud detection, and PDF receipt generation with email notifications.

> Academic project — ENSA Marrakech (ING1–ING5 collaborative modules)

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Security Model](#security-model)
- [Configuration](#configuration)
- [Running Tests](#running-tests)

---

## Overview

SecureTransfer is a backend REST API platform that simulates a banking-grade money transfer system. It supports three user types — **Particuliers** (individuals), **Agences** (agencies), and **Entreprises** (businesses) — each with its own transaction limits, KYC requirements, and role-based permissions.

The platform was built incrementally across 5 tasks:

| Task | Module | Description |
|------|--------|-------------|
| ING1 | Auth | JWT + MFA + Redis session management |
| ING2 | Users | RBAC, user profiles, AES-256 field encryption |
| ING3 | Transactions | ACID transfers, fraud detection, agency withdrawals |
| ING4 | KYC | Document upload, review workflow, state machine |
| ING5 | Documents | PDF receipt generation, email notifications (Thymeleaf) |

---

## Architecture

```
┌────────────────────────────────────────────────────────┐
│                     HTTP Requests                      │
└───────────────────────────┬────────────────────────────┘
                            │
                ┌───────────▼───────────┐
                │    JwtAuthFilter      │  ← Validates JWT + Redis blacklist
                └───────────┬───────────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
   AuthController    TransactionController   KycController
          │                 │                  │
          ▼                 ▼                  ▼
    AuthService      TransactionService   KycWorkflowService
          │                 │                  │
          └────────┬────────┘                  │
                   ▼                           ▼
             PostgreSQL (Flyway)         File Storage (kyc-documents/)
                   │
                   ▼
              Redis (token blacklist + rate limiting)
```

**Event-driven side effects** — After a transaction is created or a KYC document is reviewed, Spring application events trigger async listeners that generate PDF receipts and send email notifications without blocking the main flow.

---

## Features

### Authentication (ING1)
- User registration and login with **BCrypt** password hashing
- **JWT access tokens** (15 min) + **refresh tokens** (7 days)
- **TOTP-based MFA** via Google Authenticator with QR code generation
- **Redis token blacklist** for stateless logout
- Account lockout after repeated failed login attempts

### User Management (ING2)
- Polymorphic user model: `Particulier`, `Agence`, `Entreprise`
- **RBAC** with roles (`ROLE_USER`, `ROLE_AGENCE`, `ROLE_ADMIN`) and granular permissions
- **AES-256 field-level encryption** on sensitive fields (CIN, phone, license numbers) via a JPA `AttributeConverter`
- Per-user transaction limits (daily and per-transaction)
- **Optimistic locking** (`@Version`) to prevent concurrent update races

### Transactions (ING3)
- **ACID transfers** with `SERIALIZABLE` isolation to prevent double-spending
- **Idempotency keys** to safely handle duplicate submissions
- **Fee calculation** service with tiered rate logic
- **Fraud detection** using Java 21 sealed interfaces + pattern matching (velocity checks, limit violations, suspicious identity flags)
- **Agency cash withdrawals** with HMAC-SHA256 tokens expiring in 72 hours
- IDOR protection via `@PreAuthorize("@transactionSecurity.isOwner(...)")`

### KYC (ING4)
- Document upload (up to 5 MB) with local file storage per user type
- **State machine**: `PENDING → SUBMITTED → APPROVED / REJECTED`
- Admin review workflow with approval/rejection and reason tracking
- Spring application events (`KycSubmittedEvent`, `KycReviewedEvent`) for async notifications

### Documents & Notifications (ING5)
- **PDF receipt generation** with OpenPDF per completed transaction
- **Thymeleaf HTML email templates** for transaction confirmations and KYC status updates
- Email delivery via Spring Mail (Gmail SMTP)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security, JJWT 0.12.6 |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL 16 |
| Migrations | Flyway |
| Caching | Spring Data Redis 7 |
| PDF | OpenPDF 2.0.3 |
| Email | Spring Boot Mail + Thymeleaf |
| QR Code | ZXing 3.5.3 |
| Mapping | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Testing | JUnit 5, Mockito, Spring Security Test |
| Build | Maven |
| Containers | Docker / Docker Compose |

---

## Project Structure

```
SecureTransfer-Platform/
├── docker-compose.yml              # PostgreSQL + Redis containers
├── .env                            # Environment secrets (not committed)
└── platform/
    ├── pom.xml
    └── src/main/java/com/securetransfer/platform/
        ├── PlatformApplication.java
        ├── config/
        │   ├── SecurityConfig.java         # Spring Security filter chain
        │   └── JwtAuthFilter.java          # JWT validation per request
        ├── common/
        │   ├── exception/                  # BusinessException, ResourceNotFoundException
        │   └── util/
        │       └── EncryptedStringConverter.java  # AES-256 JPA converter
        ├── security/
        │   ├── JwtService.java             # Token generation & validation
        │   └── MfaService.java             # TOTP + QR code
        ├── controller/
        │   └── AuthController.java         # /api/auth/**
        ├── service/
        │   ├── AuthService.java
        │   └── UserDetailsServiceImpl.java
        ├── entity/
        │   └── UserCredential.java
        ├── repository/
        │   └── UserRepository.java
        ├── dto/
        │   ├── LoginRequest.java
        │   ├── RegisterRequest.java
        │   ├── AuthResponse.java
        │   └── MfaActivationResponse.java
        ├── user/
        │   ├── controller/UserController.java
        │   ├── entity/                     # BaseUser, Particulier, Agence, Entreprise
        │   ├── dto/                        # Create/Update/Response DTOs
        │   ├── service/UserService.java
        │   ├── repository/                 # One repo per user type
        │   └── mapper/UserMapper.java
        ├── transaction/
        │   ├── controller/TransactionController.java
        │   ├── entity/                     # Transaction, FraudAlert, enums
        │   ├── dto/                        # TransactionRequest/Response, WithdrawalRequest
        │   ├── service/                    # TransactionService, FeeCalculation, Withdrawal
        │   ├── fraud/                      # FraudDetectionService, FraudResult (sealed)
        │   ├── security/TransactionSecurityBean.java
        │   ├── repository/
        │   └── event/TransactionCreatedEvent.java
        ├── kyc/
        │   ├── controller/KycController.java
        │   ├── entity/                     # KycDocument, KycDocumentState
        │   ├── service/KycWorkflowService.java
        │   ├── repository/KycDocumentRepository.java
        │   ├── event/                      # KycSubmittedEvent, KycReviewedEvent
        │   └── exception/GlobalExceptionHandler.java
        └── document/
            ├── service/
            │   ├── PdfReceiptService.java  # OpenPDF receipt generation
            │   └── NotificationService.java # Email dispatch via Thymeleaf
            └── listener/
                ├── TransactionEventListener.java
                └── KycEventListener.java
    └── src/main/resources/
        ├── application.yml
        ├── db/migration/
        │   ├── V1__create_user_credentials.sql
        │   ├── V2__users.sql
        │   ├── V3__transactions.sql
        │   └── V4__kyc_documents.sql
        └── templates/email/
            ├── transaction-confirmation.html
            ├── kyc-submitted.html
            ├── kyc-approved.html
            └── kyc-rejected.html
```

---

## Database Schema

```
user_credentials          (UUID PK, email, password, mfa_secret, account_locked)
    │
    └── linked by email
          │
    ┌─────┴──────┬──────────────┐
    ▼            ▼              ▼
particuliers   agences      entreprises
(CIN enc.)  (license enc.) (reg_number)
    │            │              │
    └─────────── user_roles ────┘
                    │
                  roles ── role_permissions ── permissions

transactions
    ├── sender_id  (→ any user type)
    ├── receiver_id
    ├── idempotency_key (UNIQUE)
    └── fraud_alerts (1-to-many)

kyc_documents
    ├── user_id
    ├── user_type  (PARTICULIER / AGENCE / ENTREPRISE)
    ├── state      (PENDING / SUBMITTED / APPROVED / REJECTED)
    └── file_path
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1. Start infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL 16 on port `5432` and Redis 7 on port `6379`.

### 2. Configure the application

Edit `platform/src/main/resources/application.yml` and set your values (or use the `.env` approach if your environment picks it up):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/securetransfer
    username: root
    password: your_db_password

  mail:
    host: smtp.gmail.com
    port: 587
    username: your_gmail@gmail.com
    password: your_gmail_app_password   # Generate at myaccount.google.com/apppasswords

jwt:
  secret: <64-char hex string>

encryption:
  key: <base64 AES-256 key>
```

> **Note:** Gmail requires a 16-character **App Password** (2FA must be enabled on your Google account). The regular account password will not work.

### 3. Build and run

```bash
cd platform
./mvnw spring-boot:run
```

Flyway will automatically apply all migrations (`V1` through `V4`) on first startup.

The API is available at `http://localhost:8080`.

---

## API Reference

### Authentication — `/api/auth`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/register` | Register a new user |
| `POST` | `/login` | Login, returns JWT + refresh token |
| `POST` | `/mfa/activate` | Activate TOTP MFA, returns QR code |
| `POST` | `/mfa/verify` | Verify TOTP code |
| `POST` | `/refresh` | Refresh access token |
| `POST` | `/logout` | Blacklist current token in Redis |

### Users — `/api/users`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/particulier` | Create a Particulier account |
| `POST` | `/agence` | Create an Agence account |
| `POST` | `/entreprise` | Create an Entreprise account |
| `GET` | `/{id}` | Get user profile |
| `PUT` | `/{id}` | Update user profile |

### Transactions — `/api/transactions`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Initiate a transfer |
| `GET` | `/{id}` | Get transaction details (owner only) |
| `POST` | `/withdraw` | Request agency cash withdrawal token |
| `POST` | `/withdraw/confirm` | Confirm withdrawal with HMAC token |

### KYC — `/api/kyc`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/submit` | Upload KYC document |
| `GET` | `/status` | Check own KYC status |
| `POST` | `/review/{id}` | Admin: approve or reject a document |

---

## Security Model

### JWT Flow

```
Client → POST /login → { accessToken (15min), refreshToken (7d) }
Client → Authorization: Bearer <accessToken> on all subsequent requests
Client → POST /refresh with refreshToken → new accessToken
Client → POST /logout → accessToken added to Redis blacklist
```

### MFA Flow

```
POST /mfa/activate → returns otpauthUrl + QR code (base64 PNG)
User scans with Google Authenticator
POST /mfa/verify { code: "123456" } → MFA confirmed
From that point, login requires a second step with the 6-digit TOTP code
```

### Field Encryption

Sensitive columns (`cin`, `phone_number`, `license_number`) are transparently encrypted at rest using `EncryptedStringConverter`, a JPA `AttributeConverter` backed by AES-256-GCM. The key is loaded from `encryption.key` in `application.yml`.

### RBAC Permissions

| Role | Permissions |
|------|-------------|
| `ROLE_USER` | `user:read`, `transaction:initiate`, `transaction:view`, `kyc:submit` |
| `ROLE_AGENCE` | All of the above + `user:write` |
| `ROLE_ADMIN` | All permissions + `kyc:validate` |

---

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP port |
| `jwt.expiration` | `900000` (15 min) | Access token TTL in ms |
| `jwt.refresh-expiration` | `604800000` (7 days) | Refresh token TTL in ms |
| `kyc.storage-dir` | `kyc-documents` | Local directory for uploaded KYC files |
| `receipt.storage-dir` | `/home/ollama/receipts` | Local directory for generated PDF receipts |
| `spring.servlet.multipart.max-file-size` | `5MB` | Max KYC document size |

---

## Running Tests

```bash
cd platform
./mvnw test
```

Test coverage includes:

| Test Class | Coverage |
|-----------|----------|
| `FraudDetectionServiceTest` | Velocity, limit, and identity fraud scenarios |
| `FeeCalculationServiceTest` | Tiered fee precision with `BigDecimal` |
| `KycWorkflowServiceTest` | State machine transitions and event publishing |
| `UserServiceTest` | User creation, validation, and duplicate handling |

---

## Notes

- **Credentials in `application.yml`** — For development, SMTP credentials are hardcoded. For production, externalize them via environment variables or a secrets manager.
- **KYC file storage** — Files are stored on the local filesystem. For production, replace with object storage (S3, MinIO, etc.).
- **Receipt storage path** — `receipt.storage-dir` defaults to `/home/ollama/receipts`; update this to a path that exists on your server.
- **Flyway conflicts** — If you reset the database, drop the `flyway_schema_history` table before restarting, or set `baseline-on-migrate: true` (already configured).
