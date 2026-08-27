interface JwtPayload {
  exp?: number;
}

const normalizeBase64 = (input: string): string => {
  let base64 = input.replace(/-/g, "+").replace(/_/g, "/");
  while (base64.length % 4 !== 0) base64 += "=";
  return base64;
};

function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const segment = token.split(".")[1];
    if (!segment) return null;
    return JSON.parse(atob(normalizeBase64(segment))) as JwtPayload;
  } catch {
    return null;
  }
}

/**
 * Detects an already-expired token at load time (e.g. the tab was left open
 * past the token's 15-minute lifetime) instead of only reacting after the
 * first failed request.
 */
export function isJwtExpired(token: string): boolean {
  const payload = decodeJwtPayload(token);
  if (!payload || typeof payload.exp !== "number") return true;
  return Date.now() >= payload.exp * 1000;
}
