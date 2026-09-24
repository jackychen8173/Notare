"use client";

import { useEffect, useRef, useState } from "react";
import Script from "next/script";
import { useRouter } from "next/navigation";
import axios from "axios";

import { api } from "@/lib/api";
import { saveSession } from "@/lib/auth";
import type { AuthSession, UserRole } from "@/types/user";

// Google Identity Services isn't published with first-party types and this app has no other
// third-party script yet to have already declared this - minimal ambient shape for just the two
// calls this component makes.
declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential: string }) => void;
          }) => void;
          renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
        };
      };
    };
  }
}

const ROLE_HOME: Record<AuthSession["role"], string> = {
  TUTOR: "/dashboard",
  STUDENT: "/student/dashboard",
  ADMIN: "/admin/dashboard",
};

interface GoogleSignInButtonProps {
  endpoint: "/api/auth/google/login" | "/api/auth/google/register";
  // Only meaningful for the register endpoint - the login endpoint ignores it server-side.
  role?: UserRole;
  // e.g. the register page's Google button, before a role has been chosen.
  disabled?: boolean;
}

export function GoogleSignInButton({ endpoint, role, disabled }: GoogleSignInButtonProps) {
  const router = useRouter();
  const containerRef = useRef<HTMLDivElement>(null);
  // GSI's callback is registered once (in initialize, below) and would otherwise close over
  // whatever `role` was current on that first render - a ref keeps it live across role changes
  // without re-initializing the widget.
  const roleRef = useRef(role);
  const [scriptLoaded, setScriptLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    roleRef.current = role;
  }, [role]);

  useEffect(() => {
    if (!scriptLoaded || !containerRef.current || !window.google) return;

    window.google.accounts.id.initialize({
      client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? "",
      callback: async (response) => {
        setError(null);
        try {
          const res = await api.post<{ success: boolean; data: AuthSession }>(endpoint, {
            idToken: response.credential,
            role: roleRef.current,
          });
          const session = res.data.data;
          saveSession(session);
          router.push(ROLE_HOME[session.role]);
        } catch (err) {
          if (axios.isAxiosError<{ message?: string }>(err)) {
            setError(err.response?.data?.message ?? "Something went wrong. Try again.");
          } else {
            setError("Something went wrong. Try again.");
          }
        }
      },
    });
    window.google.accounts.id.renderButton(containerRef.current, {
      theme: "outline",
      size: "large",
      width: 320,
    });
  }, [scriptLoaded, endpoint, router]);

  return (
    <div className="flex flex-col gap-2">
      <Script
        src="https://accounts.google.com/gsi/client"
        strategy="afterInteractive"
        onLoad={() => setScriptLoaded(true)}
      />
      <div
        ref={containerRef}
        className={disabled ? "pointer-events-none opacity-50" : undefined}
      />
      {error ? <p className="text-xs text-destructive">{error}</p> : null}
    </div>
  );
}
