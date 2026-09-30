"use client";

import { useState } from "react";
import { IconArrowLeft, IconFlask } from "@tabler/icons-react";

import { useSignOut } from "@/hooks/useSignOut";
import { useStartDemo } from "@/hooks/useStartDemo";
import { getSession } from "@/lib/auth";

/** Shown across the top of every page for one-click demo accounts. */
export function DemoBanner() {
  const signOut = useSignOut();
  const startDemo = useStartDemo();
  const [session] = useState(() => getSession());
  if (session?.demo !== true) return null;

  const otherRole = session.role === "TUTOR" ? "STUDENT" : "TUTOR";

  return (
    <div className="flex flex-wrap items-center justify-center gap-x-4 gap-y-1 border-b border-border bg-primary-soft px-4 py-2 text-center text-sm text-foreground">
      {/* The landing page sends signed-in users to their dashboard, so leaving means ending the demo. */}
      <button
        type="button"
        onClick={() => signOut("/")}
        className="inline-flex items-center gap-1 font-medium text-primary underline-offset-4 hover:underline"
      >
        <IconArrowLeft className="size-4" stroke={1.75} aria-hidden />
        Exit demo
      </button>
      <span className="inline-flex items-center gap-1.5">
        <IconFlask className="size-4 text-primary" stroke={1.75} aria-hidden />
        You&apos;re exploring a demo classroom with sample data. Sage and code Run are turned off.
      </span>
      <button
        type="button"
        disabled={startDemo.isPending}
        onClick={() => startDemo.mutate(otherRole)}
        className="font-medium text-primary underline-offset-4 hover:underline disabled:opacity-60"
      >
        {startDemo.isPending ? "Switching..." : `Try it as a ${otherRole === "TUTOR" ? "teacher" : "student"}`}
      </button>
      <button
        type="button"
        onClick={() => signOut("/register")}
        className="font-medium text-primary underline-offset-4 hover:underline"
      >
        Create a free account
      </button>
    </div>
  );
}
