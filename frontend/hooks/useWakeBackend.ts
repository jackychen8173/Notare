"use client";

import { useEffect } from "react";

import { api } from "@/lib/api";

/**
 * Fire-and-forget request to the backend's public health check when a page loads. The backend
 * runs on Railway in Serverless mode, so after 10 idle minutes it sleeps and the next request
 * waits ~10-15s for the JVM to start. Calling this from the pages people land on (landing,
 * login, register) starts that wake-up while they're still reading, instead of after they click
 * "Try it" or "Sign in". Failures are ignored: this is only a nudge.
 */
export function useWakeBackend() {
  useEffect(() => {
    api.get("/api/health").catch(() => {});
  }, []);
}
