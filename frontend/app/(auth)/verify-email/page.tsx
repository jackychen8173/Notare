"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { api, errorMessage } from "@/lib/api";
import { getSession, saveSession } from "@/lib/auth";

type State = { status: "working" } | { status: "done" } | { status: "failed"; message: string };

export default function VerifyEmailPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6">
      <Card className="w-full max-w-sm">
        {/* useSearchParams needs a Suspense boundary so the rest of the page can prerender. */}
        <Suspense fallback={<CardHeader><CardTitle className="text-xl">Confirming your email...</CardTitle></CardHeader>}>
          <VerifyEmail />
        </Suspense>
      </Card>
    </div>
  );
}

function VerifyEmail() {
  const token = useSearchParams().get("token");
  const [state, setState] = useState<State>(
    token ? { status: "working" } : { status: "failed", message: "This link is incomplete. Open it from the email again." },
  );
  // The link is single-use, so guard against React running the effect twice in development.
  const sent = useRef(false);

  useEffect(() => {
    if (!token || sent.current) return;
    sent.current = true;
    api
      .post("/api/auth/verify-email/confirm", { token })
      .then(() => {
        const session = getSession();
        if (session) saveSession({ ...session, emailVerified: true });
        setState({ status: "done" });
      })
      .catch((error) => setState({ status: "failed", message: errorMessage(error) }));
  }, [token]);

  const signedIn = typeof window !== "undefined" && getSession() !== null;

  return (
    <>
      <CardHeader>
        <CardTitle className="text-xl">
          {state.status === "working" ? "Confirming your email..." : state.status === "done" ? "Email confirmed" : "Couldn't confirm your email"}
        </CardTitle>
        {state.status === "done" ? (
          <CardDescription>Thanks. Password resets and class updates will reach you here.</CardDescription>
        ) : state.status === "failed" ? (
          <CardDescription>
            {state.message} You can send a new link from your profile page.
          </CardDescription>
        ) : null}
      </CardHeader>
      {state.status !== "working" ? (
        <CardContent>
          <Link href={signedIn ? "/" : "/login"} className="text-sm text-foreground underline underline-offset-4">
            {signedIn ? "Go to Notare" : "Sign in"}
          </Link>
        </CardContent>
      ) : null}
    </>
  );
}
