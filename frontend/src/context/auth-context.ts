import { createContext, useContext } from "react";
import type { AuthResponse } from "../types/models";

export interface AuthUser {
  username: string;
  role: AuthResponse["role"];
}

export interface AuthContextType {
  isAuthenticated: boolean;
  user: AuthUser | null;
  token: string | null;
  login: (auth: AuthResponse) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
};
