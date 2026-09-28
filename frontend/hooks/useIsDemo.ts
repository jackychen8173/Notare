"use client";

import { useState } from "react";

import { getSession } from "@/lib/auth";

/**
 * True for one-click demo accounts, where Sage and code Run are turned off (the backend returns 403
 * for them too). Only used inside AppShell pages, which render client-side after the auth guard.
 */
export function useIsDemo(): boolean {
  const [demo] = useState(() => getSession()?.demo === true);
  return demo;
}
