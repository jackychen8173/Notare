import axios from "axios";

import { clearSession, getToken } from "@/lib/auth";

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
    if (error.response?.status === 401) {
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
