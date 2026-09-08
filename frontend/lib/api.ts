import axios from "axios";

import { clearSession, getToken } from "@/lib/auth";

export interface ApiEnvelope<T> {
  success: boolean;
  message: string | null;
  data: T;
}

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // /api/auth/** is permitAll - a 401 there means bad credentials on that request,
    // not an expired session, and the caller (e.g. the login form) already handles it inline.
    const isAuthEndpoint = error.config?.url?.startsWith("/api/auth/");
    if (error.response?.status === 401 && !isAuthEndpoint) {
      clearSession();
      if (typeof window !== "undefined") {
        // Hard navigation, not useRouter() - this interceptor runs outside React,
        // and a full reload also clears any stale client state from the expired session.
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  },
);
