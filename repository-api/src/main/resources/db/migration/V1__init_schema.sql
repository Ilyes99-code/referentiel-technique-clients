-- =================================================================================
-- V1__init_schema.sql
-- Projet: Plateforme Sécurisée de Référentiel Technique Clients (PostgreSQL)
--
-- Scope: only the tables the application actually maps. The original script also
-- declared sites_client, serveurs, bases_donnees, firewalls, regles_nat,
-- publications_externes and certificats_ssl — an infrastructure model that was
-- designed up front but never given entities, repositories or endpoints. They were
-- removed rather than left in place, because an empty table that nothing reads is
-- indistinguishable from a table whose feature is broken, and `ddl-auto=validate`
-- gives no warning either way.
--
-- Re-adding any of them is a new migration (V6+) alongside the entity that needs it.
--
-- Table -> entity:
--   clients          -> Client
--   users            -> User
--   applications     -> ClientModule   (JPA entity name differs from the table)
--   acces_techniques -> TechnicalAccess
-- (acces_technique_images -> AccessImage is created in V5.)
-- =================================================================================

-- 1. CLIENTS — the aggregate root; everything else cascades from it.
CREATE TABLE clients (
    id     BIGSERIAL PRIMARY KEY,
    nom    VARCHAR(255) NOT NULL,
    statut VARCHAR(50)  NOT NULL -- EN_REGLE, SUSPENDU
);

-- 2. USERS — authentication. Not in the original script; added when login was built.
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL, -- ADMIN for now
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 3. APPLICATIONS — modules installed for a client (entity: ClientModule).
CREATE TABLE applications (
    id           BIGSERIAL PRIMARY KEY,
    app_name     VARCHAR(255) NOT NULL,
    version      VARCHAR(100),
    publication  VARCHAR(100), -- Enum: NAT_DIRECT, REVERSE_PROXY, etc. — not yet mapped
    date_maj     DATE,
    date_mep     DATE,
    lien_externe VARCHAR(500),
    lien_interne VARCHAR(500),
    client_id    BIGINT,
    CONSTRAINT fk_app_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

-- 4. ACCÈS TECHNIQUES — credentials vault entries (entity: TechnicalAccess).
--
-- The original had serveur_id and firewall_id alongside client_id, so one access
-- record could hang off a server, a firewall or a client. Those two columns went
-- with the tables they referenced; the entity only ever populated client_id, and a
-- three-way optional parent is ambiguous to query anyway.
--
-- `password` is ciphertext, not plaintext: EncryptedStringConverter applies
-- AES-256-GCM on write and decrypts on read, which is why the column is TEXT rather
-- than sized to a credential's length.
CREATE TABLE acces_techniques (
    id                    BIGSERIAL PRIMARY KEY,
    type_acces            VARCHAR(50), -- VPN, RDP, SSH
    description           VARCHAR(255),
    adresse               VARCHAR(255),
    port                  INTEGER,
    utilisateur_reference VARCHAR(100),
    password              TEXT,
    notes                 TEXT,
    client_id             BIGINT,
    CONSTRAINT fk_acces_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);
