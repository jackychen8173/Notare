"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Script from "next/script";
import { useRouter } from "next/navigation";
import axios from "axios";

import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import { saveSession } from "@/lib/auth";
import type { AuthSession, GoogleAuthResult, UserRole } from "@/types/user";

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

// A single combined button: one click logs an existing account straight in. A brand-new Google
// account gets an inline "which role" prompt instead of a second Google popup - the idToken
// verified on the first request is just re-sent once a role is chosen.
export function GoogleSignInButton() {
  const router = useRouter();
  const containerRef = useRef<HTMLDivElement>(null);
  const [scriptLoaded, setScriptLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingIdToken, setPendingIdToken] = useState<string | null>(null);
  const [submittingRole, setSubmittingRole] = useState(false);

  const submit = useCallback(
    async (idToken: string, role?: UserRole) => {
      setError(null);
      try {
        const res = await api.post<{ success: boolean; data: GoogleAuthResult }>("/api/auth/google", {
          idToken,
          role,
        });
        const result = res.data.data;
        if (result.needsRole) {
          setPendingIdToken(idToken);
          return;
        }
        if (result.session) {
          saveSession(result.session);
          router.push(ROLE_HOME[result.session.role]);
        }
      } catch (err) {
        if (axios.isAxiosError<{ message?: string }>(err)) {
          setError(err.response?.data?.message ?? "Something went wrong. Try again.");
        } else {
          setError("Something went wrong. Try again.");
        }
      }
    },
    [router],
  );

  useEffect(() => {
    if (!scriptLoaded || !containerRef.current || !window.google) return;

    window.google.accounts.id.initialize({
      client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? "",
      callback: (response) => {
        submit(response.credential);
      },
    });
    window.google.accounts.id.renderButton(containerRef.current, {
      theme: "outline",
      size: "large",
      width: 320,
    });
  }, [scriptLoaded, submit]);

  async function chooseRole(role: UserRole) {
    if (!pendingIdToken) return;
    setSubmittingRole(true);
    await submit(pendingIdToken, role);
    setSubmittingRole(false);
  }

  if (pendingIdToken) {
    return (
      <div className="flex flex-col gap-2 rounded-card border-hairline border-border p-3">
        <p className="text-sm text-foreground">Welcome! Are you a teacher or a student?</p>
        <div className="flex gap-2">
          <Button
            type="button"
            variant="outline"
            disabled={submittingRole}
            onClick={() => chooseRole("TUTOR")}
            className="flex-1"
          >
            Teacher
          </Button>
          <Button
            type="button"
            variant="outline"
            disabled={submittingRole}
            onClick={() => chooseRole("STUDENT")}
            className="flex-1"
          >
            Student
          </Button>
        </div>
        {error ? <p className="text-xs text-destructive">{error}</p> : null}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <Script
        src="https://accounts.google.com/gsi/client"
        strategy="afterInteractive"
        onLoad={() => setScriptLoaded(true)}
      />
      <div ref={containerRef} />
      {error ? <p className="text-xs text-destructive">{error}</p> : null}
    </div>
  );
}
