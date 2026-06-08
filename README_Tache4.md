# 🔐 Module KYC & Documents

> **SecureTransfer Platform** · Java 21 · Spring Boot 3.3 · Module `kyc.*`

---
# Overview 
## KYC Module Overview

KYC (Know Your Customer) is a digital identity verification process commonly used in banking, fintech, and financial platforms to verify users and ensure compliance with security and regulatory requirements.

In modern software architecture, KYC is not just a simple feature — it is often a complete software ecosystem composed of:

* Backend services
* REST APIs
* Databases
* Document processing
* OCR / AI verification
* Security and compliance rules
* Workflow management systems

Large-scale applications usually separate KYC into its own module or microservice for better scalability, maintainability, and security.

---

## KYC Architecture in Spring Boot

The KYC module follows a layered architecture commonly used in Spring Boot applications:

```text
Frontend
   ↓
REST API
   ↓
Controller
   ↓
Service Layer
   ↓
Repository
   ↓
Database
```

### Request Flow

```text
Frontend upload
   ↓
Controller receives request
   ↓
Service handles business logic
   ↓
Repository communicates with database
   ↓
Entity represents stored data
```

---

## Spring Boot Components Used

| Component           | Role                                   |
| ------------------- | -------------------------------------- |
| `@RestController`   | Exposes REST API endpoints             |
| `@Service`          | Contains business logic                |
| `@Repository`       | Handles database access                |
| `@Entity`           | Represents database tables             |
| `DTO`               | Transfers request/response data        |
| `@ControllerAdvice` | Global exception handling              |
| `JPA/Hibernate`     | ORM framework for database persistence |

---

## KYC Module Structure

The KYC module was designed as an isolated feature module containing its own layers and components.

```text
kyc/
├── controller
├── service
├── repository
├── entity
├── event
├── exception
└── tests
```

## Package Structure

```
com.securetransfer.platform.kyc/
├── controller/
│   └── KycController.java           ← REST endpoints, @PreAuthorize guards
├── entity/
│   ├── KycDocument.java             ← JPA entity, full audit fields
│   └── KycDocumentState.java        ← State enum: PENDING → SUBMITTED → VERIFIED/REJECTED
├── event/
│   ├── KycSubmittedEvent.java       ← Published on document submission (ING5 listens)
│   └── KycReviewedEvent.java        ← Published on admin decision (ING5 listens)
├── exception/
│   └── GlobalExceptionHandler.java  ← @ControllerAdvice, covers all modules
├── repository/
│   └── KycDocumentRepository.java   ← JPA queries, state filters
└── service/
    └── KycWorkflowService.java      ← State machine, SHA-256, MIME detection, storage
```
---

## Key Concepts Learned

During the implementation of this module, research was conducted on:

* Spring Boot layered architecture
* Feature-based modular organization
* REST API design
* JWT authentication and security
* JPA/Hibernate persistence
* Multipart file uploads
* Event-driven workflows
* SHA-256 file hashing
* KYC verification systems
* Enterprise backend structure
* Microservice-oriented architecture

---

## Why This Architecture Matters

This architecture provides:

* Scalability
* Separation of concerns
* Maintainability
* Security isolation
* Easier teamwork and collaboration
* Cleaner feature organization

Each module can evolve independently without affecting the rest of the application.


## Database

**Migration:** `V4__kyc_documents.sql`



## State Machine

The KYC workflow enforces a strict one-way state progression. Any transition that violates the machine throws a `BusinessException` — no silent state corruption is possible.

```
  [PENDING]
      │
      │  user submits document
      ▼
 [SUBMITTED]  ──── admin verifies ────▶  [VERIFIED]
                │
                └── admin rejects ────▶  [REJECTED]
```

State transitions are wrapped in `@Transactional` — partial updates are impossible.  
After each admin decision, the parent user's `KycStatus` field (managed by ING2) is updated automatically.

---

## Security Implementation

### File Upload — Defense in Depth

The module applies three independent layers of validation before any file touches disk:

| Layer | Check | How |
|-------|-------|-----|
| 1 | MIME type | Magic bytes (first 4 bytes), NOT the `Content-Type` header |
| 2 | File size | Hard limit of 5 MB |
| 3 | Duplicate detection | SHA-256 hash compared against all stored hashes |

**MIME detection reads the actual file bytes**, not the declared content type — preventing extension spoofing (e.g. a `.exe` renamed to `.jpg`):

```java
// JPEG:  FF D8 FF
// PNG:   89 50 4E 47
// PDF:   25 50 44 46
```

### Storage

Files are stored **outside the webroot** under `kyc-documents/{userType}/{userId}/`, making them inaccessible via direct URL. All filenames are sanitized and timestamped to prevent path traversal.

### Integrity Verification

On every read (`getDocumentWithIntegrityCheck`), the SHA-256 of the file on disk is recomputed and compared against the stored hash. A mismatch means the file was tampered with — the API returns a `400` and logs a security alert:


### Access Control

Every endpoint has explicit `@PreAuthorize` — no endpoint is reachable without the correct role:

| Endpoint | Role required |
|----------|--------------|
| `POST /kyc/submit` | `USER`, `AGENCE`, or `ENTREPRISE` |
| `GET /kyc/my-documents` | `USER`, `AGENCE`, or `ENTREPRISE` |
| `GET /kyc/pending` | `ADMIN` only |
| `POST /kyc/{id}/verify` | `ADMIN` only |
| `POST /kyc/{id}/reject` | `ADMIN` only |
| `GET /kyc/{id}/integrity` | `ADMIN` only |

---

## REST API Reference

### Submit a document
```http
POST /api/v1/kyc/submit
Authorization: Bearer <token>
Content-Type: multipart/form-data

Fields:
  userId       (Long)    — user's database id
  userType     (String)  — PARTICULIER | AGENCE | ENTREPRISE
  documentType (String)  — CIN | PASSPORT | LICENSE
  file         (File)    — JPEG, PNG, or PDF, max 5MB
```

**Response `200`:**
```json
{
  "id": 1,
  "userId": 1,
  "userType": "PARTICULIER",
  "documentType": "CIN",
  "fileName": "1748166961_cin.jpg",
  "mimeType": "image/jpeg",
  "fileSize": 12,
  "state": "SUBMITTED",
  "rejectionReason": null,
  "submittedAt": "2026-05-25T10:36:01"
}
```

### List my documents
```http
GET /api/v1/kyc/my-documents?userId=1&userType=PARTICULIER
Authorization: Bearer <token>
```

### List pending documents (admin)
```http
GET /api/v1/kyc/pending
Authorization: Bearer <admin_token>
```

### Verify a document (admin)
```http
POST /api/v1/kyc/{documentId}/verify?userEmail=user@email.com
Authorization: Bearer <admin_token>
```

### Reject a document (admin)
```http
POST /api/v1/kyc/{documentId}/reject?userEmail=user@email.com
Authorization: Bearer <admin_token>
Content-Type: application/json

{ "reason": "Document illisible" }
```

### Check file integrity (admin)
```http
GET /api/v1/kyc/{documentId}/integrity
Authorization: Bearer <admin_token>
```

**Response `200`:**
```json
{
  "documentId": 1,
  "integrityOk": true,
  "hash": "a3f1c2d4...",
  "state": "VERIFIED"
}
```

---

## GlobalExceptionHandler

This module owns `GlobalExceptionHandler.java` (`@RestControllerAdvice`), which covers the **entire application** — not just KYC. It handles:

| Exception | HTTP Status | Notes |
|-----------|-------------|-------|
| `BusinessException` | `400 Bad Request` | Domain rule violations |
| `ResourceNotFoundException` | `404 Not Found` | Entity not found |
| `AccessDeniedException` | `403 Forbidden` | Spring Security — no stack trace exposed |
| `MethodArgumentNotValidException` | `400` | Bean Validation, field-level errors |
| `MaxUploadSizeExceededException` | `413 Payload Too Large` | File size limit |
| `Exception` (catch-all) | `500 Internal Server Error` | Logs full stack, returns generic message |

**No stack trace or internal detail is ever sent to the client.**

---

## Unit Tests

**File:** `src/test/java/com/securetransfer/platform/kyc/KycWorkflowServiceTest.java`

| Test | What it validates |
|------|-------------------|
| `submitDocument_invalidMimeType_throwsBusinessException` | Non-JPEG/PNG/PDF files rejected by magic bytes |
| `submitDocument_fileTooLarge_throwsBusinessException` | Files > 5MB refused before touching disk |
| `submitDocument_validJpeg_savesDocument` | Happy path: document saved, event published, hash stored |
| `submitDocument_duplicateHash_throwsBusinessException` | Same file submitted twice is blocked |
| `verifyDocument_notSubmitted_throwsBusinessException` | State machine: can't verify a PENDING doc |
| `verifyDocument_submittedDocument_setsVerifiedState` | Happy path: state → VERIFIED, admin recorded |
| `rejectDocument_submittedDocument_setsRejectedWithReason` | Rejection stores reason and admin identity |

Run tests:
```bash
mvn test -pl platform
```

---

## Configuration

Add to `application.yml`:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB
```

No additional dependencies were added to `pom.xml` — everything uses libraries already present in the project (Spring Web, Spring Data JPA, Spring Security, Lombok).

---

## Integration Points

| Module | Dependency | Direction |
|--------|-----------|-----------|
| ING1 (Auth) | JWT token required on all endpoints | ING1 → ING4 |
| ING2 (Users) | Updates `KycStatus` on `Particulier`, `Agence`, `Entreprise` after review | ING4 → ING2 |
| ING3 (Transactions) | Reads user `KycStatus` before allowing transfers (no direct call to KYC) | ING3 → ING2 |
| ING5 (Notifications) | Listens to `KycSubmittedEvent` and `KycReviewedEvent` | ING4 → ING5 |
| ING6 (Agency) | No direct dependency | — |

---

## Key Design Decisions

**Why magic bytes instead of `Content-Type`?**  
The declared MIME type in a multipart request is set by the client and can be anything. Reading the first bytes of the actual file is the only reliable way to detect the real format.

**Why SHA-256 on every read, not just on write?**  
Documents are legal evidence. A hash stored at upload time proves what was submitted. Recomputing on read proves the file hasn't been modified on disk since — detecting both filesystem tampering and storage corruption.

**Why a separate `KycDocumentState` enum instead of reusing `KycStatus`?**  
`KycStatus` (ING2) is a user-level field summarizing the overall KYC outcome. `KycDocumentState` is a document-level field tracking one specific file through the review workflow. A user can have multiple documents in different states simultaneously.

**Why `GlobalExceptionHandler` lives in the KYC module?**  
The guide assigned it to ING4. It is `@RestControllerAdvice` so it applies application-wide regardless of its package location.

---
# How to test

**Prerequisites: app running (mvn spring-boot:run), Docker up, user already registered.**

1. Login and store token (token expires in 15 min — do all steps in one session):
   powershell$r = Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/v1/auth/login" `
   -ContentType "application/json" -Body '{"email":"test@test.com","password":"password123"}'
   $token = $r.token
2. Create a user profile (if not done yet):
   powershellInvoke-RestMethod -Method POST -Uri "http://localhost:8080/api/users/particuliers" `
  -ContentType "application/json" `
   -Body '{"email":"test@test.com","password":"password123","phoneNumber":"0612345678","cin":"AB123456","dateOfBirth":"1995-01-15","nationality":"Moroccan"}'
3. Submit a KYC document (multipart upload):
   powershell# Create a minimal valid JPEG
   $bytes = [byte[]](0xFF,0xD8,0xFF,0xE0,0x00,0x10,0x4A,0x46,0x49,0x46,0x00,0x01)
   [IO.File]::WriteAllBytes("C:\test_cin.jpg", $bytes)

# Build multipart body manually (PowerShell 5 compatible)
$boundary = "----Boundary" + [System.Guid]::NewGuid().ToString("N")
$enc = [System.Text.Encoding]::UTF8
$bodyLines = @(
"--$boundary", 'Content-Disposition: form-data; name="userId"', "", "1",
"--$boundary", 'Content-Disposition: form-data; name="userType"', "", "PARTICULIER",
"--$boundary", 'Content-Disposition: form-data; name="documentType"', "", "CIN",
"--$boundary", "Content-Disposition: form-data; name=`"file`"; filename=`"test_cin.jpg`"",
"Content-Type: image/jpeg", "", ""
)
$ms = New-Object System.IO.MemoryStream
$ms.Write($enc.GetBytes($bodyLines -join "`r`n"), 0, $enc.GetBytes($bodyLines -join "`r`n").Length)
$fb = [IO.File]::ReadAllBytes("C:\test_cin.jpg")
$ms.Write($fb, 0, $fb.Length)
$end = $enc.GetBytes("`r`n--$boundary--`r`n")
$ms.Write($end, 0, $end.Length)

Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/v1/kyc/submit" `
  -Headers @{Authorization="Bearer $token"} `
-ContentType "multipart/form-data; boundary=$boundary" `
-Body $ms.ToArray()
✅ Expected: response with "state": "SUBMITTED"
4. Check document status:
   powershellInvoke-RestMethod -Method GET `
  -Uri "http://localhost:8080/api/v1/kyc/my-documents?userId=1&userType=PARTICULIER" `
   -Headers @{Authorization="Bearer $token"}
5. Run unit tests:
   bashmvn test
   7 tests covering: MIME rejection, size limit, duplicate hash, state machine transitions (verify/reject), happy path.
*SecureTransfer Platform · Java 21 · Spring Boot 3.3 · Module kyc.* ·