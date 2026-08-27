// Mirrors com.clinic.repository_api.model.enums.ClientStatut's @JsonValue exactly —
// "en regle" (no accent) and "suspendu", not the French-accented spelling.
export type ClientStatut = "en regle" | "suspendu";

// Mirrors ClientDto(Long id, String nom, ClientStatut statut, String notes). The
// backend does not (yet) expose pays/ville/type/observations, so those are
// intentionally absent here.
export interface ClientDto {
  id: number;
  nom: string;
  statut: ClientStatut;
  notes: string | null;
}

export interface ClientRequest {
  nom: string;
  statut?: ClientStatut;
  notes?: string | null;
}

// Mirrors ModuleDto.
export interface ModuleDto {
  id: number;
  clientId: number;
  module: string;
  version: string | null;
  dateMaj: string | null; // ISO "YYYY-MM-DD", matches <input type="date"> directly
  dateMep: string | null;
  lienExterne: string | null;
  lienInterne: string | null;
}

export interface ModuleRequest {
  module: string;
  version: string;
  dateMaj: string;
  dateMep: string;
  lienExterne: string;
  lienInterne: string;
}

// Mirrors TechnicalAccessDto. Note this is a flat, generic record — the backend has
// no fixed set of access "types"; `type` is a free-text field and a client can have
// any number of these records.
export interface TechnicalAccessDto {
  id: number;
  clientId: number;
  type: string;
  description: string | null;
  address: string | null;
  port: number | null;
  username: string | null;
  password: string | null;
  notes: string | null;
}

export interface TechnicalAccessRequest {
  type: string;
  description: string;
  address: string;
  port: number | null;
  username: string;
  password: string;
  notes: string;
}

// Mirrors AccessImageDto — metadata only. The bytes are never inlined in JSON;
// they come from GET /clients/{id}/access-images/{imageId}/content, which requires
// the bearer token like every other endpoint (see fetchAccessImageBlob).
export interface AccessImageDto {
  id: number;
  clientId: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string; // ISO-8601 instant, e.g. "2026-08-23T09:41:02.113Z"
}

// Mirrors AuthResponse.
export interface AuthResponse {
  accessToken: string;
  username: string;
  role: "ADMIN";
}
