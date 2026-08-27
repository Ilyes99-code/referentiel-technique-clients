import React, { useEffect, useState } from "react";
import type { AuthResponse } from "../types/models";
import { AuthContext, type AuthUser } from "./auth-context";
import { setUnauthorizedHandler } from "../services/api";
import { isJwtExpired } from "../lib/jwt";

const AUTH_STORAGE_KEY = "referentiel-auth";

interface StoredAuth {
  user: AuthUser | null;
  token: string | null;
}

const readStoredAuth = (): StoredAuth => {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) return { user: null, token: null };
    const parsed = JSON.parse(raw) as Partial<StoredAuth>;
    const user = parsed.user ?? null;
    const token = parsed.token ?? null;

    // A token left over from a previous session may already be past its
    // 15-minute lifetime — treat that the same as never having logged in,
    // rather than showing a UI that looks authenticated but can't fetch anything.
    if (!user || !token || isJwtExpired(token)) {
      return { user: null, token: null };
    }
    return { user, token };
  } catch {
    return { user: null, token: null };
  }
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const storedAuth = readStoredAuth();
  const [user, setUser] = useState<AuthUser | null>(storedAuth.user);
  const [token, setToken] = useState<string | null>(storedAuth.token);

  const login = (auth: AuthResponse) => {
    const nextUser: AuthUser = { username: auth.username, role: auth.role };
    setUser(nextUser);
    setToken(auth.accessToken);
    localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({ user: nextUser, token: auth.accessToken }),
    );
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem(AUTH_STORAGE_KEY);
  };

  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{ isAuthenticated: !!user && !!token, user, token, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
};
