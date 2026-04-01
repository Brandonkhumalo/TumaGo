"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import { adminAPI } from "@/lib/api";

// Shape of the admin user object returned by the login endpoint.
interface AdminUser {
  id: string;
  email: string;
  name: string;
  is_staff: boolean;
}

// Everything the auth context exposes to consumers.
interface AuthContextType {
  token: string | null;
  user: AdminUser | null;
  isAuthenticated: boolean;
  isLoading: boolean; // true while the initial token check runs on mount
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// ── Helpers ──────────────────────────────────────────────────────────────

const TOKEN_KEY = "tumago_admin_token";
const USER_KEY = "tumago_admin_user";

/**
 * Decode the payload section of a JWT (base64url) without pulling in a
 * library.  Returns the parsed JSON object or null on failure.
 */
function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;

    // base64url -> base64 -> decoded string
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = atob(base64);
    return JSON.parse(json);
  } catch {
    return null;
  }
}

/**
 * Returns true if the JWT has NOT expired yet.  Adds a 60-second safety
 * margin so we never send a token that is about to expire.
 */
function isTokenValid(token: string): boolean {
  const payload = decodeJwtPayload(token);
  if (!payload || typeof payload.exp !== "number") return false;

  const nowSeconds = Math.floor(Date.now() / 1000);
  return payload.exp > nowSeconds + 60; // 60s buffer
}

// ── Provider ─────────────────────────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AdminUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  // On mount, rehydrate auth state from localStorage.
  useEffect(() => {
    try {
      const savedToken = localStorage.getItem(TOKEN_KEY);
      const savedUser = localStorage.getItem(USER_KEY);

      if (savedToken && isTokenValid(savedToken)) {
        setToken(savedToken);
        if (savedUser) {
          setUser(JSON.parse(savedUser));
        }
      } else {
        // Token is missing or expired — clear stale data.
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
      }
    } catch {
      // localStorage may throw in private / restricted contexts.
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Authenticate an admin user.  On success, persists the access token and
   * user info to localStorage and updates context state.
   */
  const login = useCallback(
    async (email: string, password: string) => {
      // adminAPI.login already throws on non-2xx responses.
      const data = await adminAPI.login(email, password);

      const accessToken: string = data.access_token || data.accessToken;
      const adminUser: AdminUser = data.user;

      // Persist to localStorage so state survives page refreshes.
      localStorage.setItem(TOKEN_KEY, accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(adminUser));

      setToken(accessToken);
      setUser(adminUser);

      router.push("/tumago/admin");
    },
    [router]
  );

  /**
   * Clear all auth state and redirect to the login page.
   */
  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
    router.push("/tumago/admin/login");
  }, [router]);

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider
      value={{ token, user, isAuthenticated, isLoading, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Hook to consume the auth context.  Must be called inside an
 * <AuthProvider>.
 */
export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an <AuthProvider>");
  }
  return ctx;
}
