import type {
  AccessImageDto,
  AuthResponse,
  ClientDto,
  ClientRequest,
  ModuleDto,
  ModuleRequest,
  TechnicalAccessDto,
  TechnicalAccessRequest,
} from "../types/models";

const API_BASE_URL = (import.meta.env.VITE_API_URL ?? "/api").replace(
  /\/$/,
  "",
);

const buildUrl = (path: string) =>
  `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;

export class ApiError extends Error {
  status: number;

  // True when `message` came from the API's JSON error body, false when it was
  // synthesised from the status code. Callers rendering an error to a user need
  // this: "Le nom du client est requis" is worth showing, "Request failed (403)"
  // is not, and only the flag tells them apart.
  fromServer: boolean;

  constructor(message: string, status: number, fromServer = false) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fromServer = fromServer;
  }
}

// Set by AuthProvider on mount so a request rejected as unauthenticated (expired/
// invalid token) can trigger a logout without api.ts depending on React/context —
// only fires for calls that were actually made with a token, so a failed login
// attempt (never has a token) never triggers it.
let onUnauthorized: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler;
}

// Matches GlobalExceptionHandler's JSON error shape: { message, errors? }
async function readErrorMessage(
  response: Response,
): Promise<{ message: string; fromServer: boolean }> {
  try {
    const body = (await response.clone().json()) as { message?: string };
    if (body?.message) return { message: body.message, fromServer: true };
  } catch {
    // response wasn't JSON — fall through to a generic message
  }
  return { message: `Request failed (${response.status})`, fromServer: false };
}

// Shared transport: auth header, 401 handling and error-body decoding. Returns the
// raw Response so non-JSON payloads (image bytes) can go through the same pipeline
// instead of re-implementing it.
async function rawRequest(
  path: string,
  init: RequestInit = {},
  token?: string,
): Promise<Response> {
  const headers: Record<string, string> = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  // FormData must set its own Content-Type — the browser appends the multipart
  // boundary, and overriding it here makes the body unparseable server-side.
  if (!(init.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(buildUrl(path), {
    ...init,
    headers: {
      ...headers,
      ...(init.headers ?? {}),
    },
  });

  if (!response.ok) {
    if (response.status === 401 && token && onUnauthorized) {
      onUnauthorized();
    }
    const { message, fromServer } = await readErrorMessage(response);
    throw new ApiError(message, response.status, fromServer);
  }

  return response;
}

async function request<T>(
  path: string,
  init: RequestInit = {},
  token?: string,
): Promise<T> {
  const response = await rawRequest(path, init, token);

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export async function login(
  username: string,
  password: string,
): Promise<AuthResponse> {
  return request<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

export async function getClients(token: string): Promise<ClientDto[]> {
  return request<ClientDto[]>("/clients", { method: "GET" }, token);
}

export async function getClient(clientId: number, token: string): Promise<ClientDto> {
  return request<ClientDto>(`/clients/${clientId}`, { method: "GET" }, token);
}

export async function updateClient(
  clientId: number,
  payload: ClientRequest,
  token: string,
): Promise<ClientDto> {
  return request<ClientDto>(
    `/clients/${clientId}`,
    { method: "PUT", body: JSON.stringify(payload) },
    token,
  );
}

export async function getModules(
  clientId: number,
  token: string,
): Promise<ModuleDto[]> {
  return request<ModuleDto[]>(`/clients/${clientId}/modules`, { method: "GET" }, token);
}

export async function createModule(
  clientId: number,
  payload: ModuleRequest,
  token: string,
): Promise<ModuleDto> {
  return request<ModuleDto>(
    `/clients/${clientId}/modules`,
    { method: "POST", body: JSON.stringify(payload) },
    token,
  );
}

export async function updateModule(
  clientId: number,
  moduleId: number,
  payload: ModuleRequest,
  token: string,
): Promise<ModuleDto> {
  return request<ModuleDto>(
    `/clients/${clientId}/modules/${moduleId}`,
    { method: "PUT", body: JSON.stringify(payload) },
    token,
  );
}

export async function getTechnicalAccessRecords(
  clientId: number,
  token: string,
): Promise<TechnicalAccessDto[]> {
  return request<TechnicalAccessDto[]>(
    `/clients/${clientId}/access-techniques`,
    { method: "GET" },
    token,
  );
}

export async function createTechnicalAccess(
  clientId: number,
  payload: TechnicalAccessRequest,
  token: string,
): Promise<TechnicalAccessDto> {
  return request<TechnicalAccessDto>(
    `/clients/${clientId}/access-techniques`,
    { method: "POST", body: JSON.stringify(payload) },
    token,
  );
}

export async function updateTechnicalAccess(
  clientId: number,
  accessId: number,
  payload: TechnicalAccessRequest,
  token: string,
): Promise<TechnicalAccessDto> {
  return request<TechnicalAccessDto>(
    `/clients/${clientId}/access-techniques/${accessId}`,
    { method: "PUT", body: JSON.stringify(payload) },
    token,
  );
}

export async function getAccessImages(
  clientId: number,
  token: string,
): Promise<AccessImageDto[]> {
  return request<AccessImageDto[]>(
    `/clients/${clientId}/access-images`,
    { method: "GET" },
    token,
  );
}

export async function uploadAccessImage(
  clientId: number,
  file: File,
  token: string,
): Promise<AccessImageDto> {
  const body = new FormData();
  body.append("file", file);

  return request<AccessImageDto>(
    `/clients/${clientId}/access-images`,
    { method: "POST", body },
    token,
  );
}

// The bytes endpoint is authenticated like everything else, and an <img src> cannot
// carry an Authorization header — so the caller fetches the blob here and renders it
// through URL.createObjectURL. Every returned blob owns an object URL that the caller
// is responsible for revoking, otherwise the decoded image stays pinned in memory for
// the lifetime of the document.
export async function fetchAccessImageBlob(
  clientId: number,
  imageId: number,
  token: string,
): Promise<Blob> {
  const response = await rawRequest(
    `/clients/${clientId}/access-images/${imageId}/content`,
    { method: "GET" },
    token,
  );
  return response.blob();
}

export async function deleteAccessImage(
  clientId: number,
  imageId: number,
  token: string,
): Promise<void> {
  await request<void>(
    `/clients/${clientId}/access-images/${imageId}`,
    { method: "DELETE" },
    token,
  );
}
