<<<<<<< HEAD
# Tâche 3 (ING3) - Moteur Transactionnel

Cette partie du projet concerne la gestion complète et sécurisée des transactions sur la plateforme SecureTransfer. Elle est isolée dans le package `com.securetransfer.platform.transaction`.

## 📌 Fonctionnalités implémentées

1. **Entités et Base de données (JPA & Flyway)**
   - Script de migration `V3__transactions.sql` pour créer les tables sans casser l'architecture des collègues.
   - Les entités `Transaction` liées avec `FraudAlert` via des Jointures JPA strictes (@ManyToOne).

2. **Logique de Transaction (ACID)**
   - **Isolation Serializable** : Assurée via l'annotation `@Transactional(isolation = Isolation.SERIALIZABLE)` afin d'empêcher les failles comme une double dépense si deux requêtes arrivent à la même milliseconde en asynchrone (Concurrency issues).
   - **Clé d'Idempotence** : Évite purement et simplement les doubles soumissions de requêtes (ex: clic répété d'un utilisateur par erreur).

3. **Détection Automatique des Fraudes**
   - Mise en place d'une interface sécurisée `FraudResult` utilisant la fonctionnalité structurée d'interfaces scellées (Sealed interface) de **Java 21**.
   - Inspection avec la méthode structurale innovante **Pattern Matching**, lissant le traitement des scénarios (fraude rejetée, identité suspecte, limite des fonds atteinte, vélocité du réseau trop grande).

4. **Retraits en Agence avec Authentification Cryptographique**
   - Implémentation du service `WithdrawalService`.
   - Création de jetons de retraits cryptés générés via `HMAC-SHA256` et limités par un délai strict d'expiration interne de 72 heures.

5. **Sécurité et Évènements IDOR / API**
   - Bloquage des failles courantes API "IDOR" (un utilisateur voyant la transaction d'un autre) par injonction d'un filtre restrictif `@PreAuthorize("@transactionSecurity.isOwner(...)")`.
   - Lancement des signaux `TransactionCreatedEvent` dans le coeur de l'application permettant aux modules suivants (ex: ING5, génération PDF) de s'enclencher de façon asynchrone.

## ⚙️ Les Tests
Les processus d'évaluation des frais `FeeCalculationService` et des alertes pour les paiements abusifs `FraudDetectionService` sont couverts par des tests locaux (Mockito). Ils certifient en permanence que les frais bancaires sont justes au centime près, grâce à l'utilisation systématique des types `BigDecimal`.

---
*Ce module dépend des entités "Users" (via ING2) et repose sur l'architecture standard sans nécessiter de modification en dehors de son contexte.*
=======
# Tâche 3 (ING3) - Moteur Transactionnel

Cette partie du projet concerne la gestion complète et sécurisée des transactions sur la plateforme SecureTransfer. Elle est isolée dans le package `com.securetransfer.platform.transaction`.

## 📌 Fonctionnalités implémentées

1. **Entités et Base de données (JPA & Flyway)**
   - Script de migration `V3__transactions.sql` pour créer les tables sans casser l'architecture des collègues.
   - Les entités `Transaction` liées avec `FraudAlert` via des Jointures JPA strictes (@ManyToOne).

2. **Logique de Transaction (ACID)**
   - **Isolation Serializable** : Assurée via l'annotation `@Transactional(isolation = Isolation.SERIALIZABLE)` afin d'empêcher les failles comme une double dépense si deux requêtes arrivent à la même milliseconde en asynchrone (Concurrency issues).
   - **Clé d'Idempotence** : Évite purement et simplement les doubles soumissions de requêtes (ex: clic répété d'un utilisateur par erreur).

3. **Détection Automatique des Fraudes**
   - Mise en place d'une interface sécurisée `FraudResult` utilisant la fonctionnalité structurée d'interfaces scellées (Sealed interface) de **Java 21**.
   - Inspection avec la méthode structurale innovante **Pattern Matching**, lissant le traitement des scénarios (fraude rejetée, identité suspecte, limite des fonds atteinte, vélocité du réseau trop grande).

4. **Retraits en Agence avec Authentification Cryptographique**
   - Implémentation du service `WithdrawalService`.
   - Création de jetons de retraits cryptés générés via `HMAC-SHA256` et limités par un délai strict d'expiration interne de 72 heures.

5. **Sécurité et Évènements IDOR / API**
   - Bloquage des failles courantes API "IDOR" (un utilisateur voyant la transaction d'un autre) par injonction d'un filtre restrictif `@PreAuthorize("@transactionSecurity.isOwner(...)")`.
   - Lancement des signaux `TransactionCreatedEvent` dans le coeur de l'application permettant aux modules suivants (ex: ING5, génération PDF) de s'enclencher de façon asynchrone.

## ⚙️ Les Tests
Les processus d'évaluation des frais `FeeCalculationService` et des alertes pour les paiements abusifs `FraudDetectionService` sont couverts par des tests locaux (Mockito). Ils certifient en permanence que les frais bancaires sont justes au centime près, grâce à l'utilisation systématique des types `BigDecimal`.

---
*Ce module dépend des entités "Users" (via ING2) et repose sur l'architecture standard sans nécessiter de modification en dehors de son contexte.*
>>>>>>> 3ba8522ccea825626175d2122bcfce25d088fc90
