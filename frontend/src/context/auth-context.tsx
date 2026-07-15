"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";

import * as authApi from "@/lib/api/auth";
import type { LoginRequest, RegisterRequest } from "@/lib/api/types";
import {
  clearAuthSession,
  getStoredProfile,
  getStoredToken,
  saveAuthSession,
  type StoredProfile,
} from "@/lib/auth-storage";

interface AuthContextValue {
  user: StoredProfile | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isHydrated: boolean;
  login: (request: LoginRequest) => Promise<StoredProfile>;
  register: (request: RegisterRequest) => Promise<StoredProfile>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<StoredProfile | null>(null);
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    const token = getStoredToken();
    const profile = getStoredProfile();
    if (token && profile) {
      setUser(profile);
    }
    setIsHydrated(true);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isAdmin: user?.role === "ADMIN",
      isHydrated,
      async login(request) {
        const response = await authApi.login(request);
        const profile = { email: response.email, fullName: response.fullName, role: response.role };
        saveAuthSession(response.token, profile);
        setUser(profile);
        return profile;
      },
      async register(request) {
        const response = await authApi.register(request);
        const profile = { email: response.email, fullName: response.fullName, role: response.role };
        saveAuthSession(response.token, profile);
        setUser(profile);
        return profile;
      },
      logout() {
        clearAuthSession();
        setUser(null);
      },
    }),
    [user, isHydrated]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
