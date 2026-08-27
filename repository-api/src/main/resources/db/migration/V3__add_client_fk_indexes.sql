-- Postgres does not automatically index foreign-key columns. Every module and
-- technical-access lookup for a client (ClientModuleRepository/TechnicalAccessRepository
-- findByClientId*) filters on these columns, so without an index each lookup is a
-- sequential scan once the tables grow past a trivial size.
CREATE INDEX idx_applications_client_id ON applications(client_id);
CREATE INDEX idx_acces_techniques_client_id ON acces_techniques(client_id);
