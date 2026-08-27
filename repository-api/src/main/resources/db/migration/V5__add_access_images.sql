-- Screenshots / visuals attached to a client's technical-access section.
--
-- The payload is stored in-row as BYTEA rather than on disk: the API runs in a
-- container, so a filesystem path would require a mounted volume plus its own backup
-- and retention story, and a deleted client would leave orphaned files behind. With
-- BYTEA the ON DELETE CASCADE below is the whole cleanup story and a normal pg_dump
-- captures everything.
--
-- Postgres TOASTs any row wider than ~2 kB out of line and compresses it, so the
-- clients-facing tables are unaffected by these blobs; the cost is that `data` must
-- never appear in a SELECT unless the bytes are actually being streamed. The
-- application enforces that with a projection (AccessImageRepository.AccessImageSummary).
CREATE TABLE acces_technique_images (
    id           BIGSERIAL PRIMARY KEY,
    client_id    BIGINT       NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    -- Derived server-side from the file's magic bytes, never from the multipart
    -- part's declared Content-Type. Constrained here as well so a future code path
    -- cannot store a type the streaming endpoint would then serve back verbatim.
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    data         BYTEA        NOT NULL,
    uploaded_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_access_image_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT chk_access_image_content_type CHECK (content_type IN ('image/png', 'image/jpeg', 'image/webp', 'image/gif')),
    CONSTRAINT chk_access_image_size CHECK (size_bytes > 0 AND size_bytes <= 5242880)
);

-- Every read path filters on client_id (gallery listing, per-image lookup), and
-- Postgres does not index foreign-key columns automatically. Same rationale as
-- V3__add_client_fk_indexes.sql.
CREATE INDEX idx_access_images_client_id ON acces_technique_images(client_id);
