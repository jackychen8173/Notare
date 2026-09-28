"use client";

import { useState } from "react";
import { IconFlask } from "@tabler/icons-react";

import { useSignOut } from "@/hooks/useSignOut";
import { getSession } from "@/lib/auth";

/** Shown across the top of every page for one-click demo accounts. */
export function DemoBanner() {
  const signOut = useSignOut();
  const [demo] = useState(() => getSession()?.demo === true);
  if (!demo) return null;

  return (
    <div className="flex flex-wrap items-center justify-center gap-x-3 gap-y-1 border-b border-border bg-primary-soft px-4 py-2 text-center text-sm text-foreground">
      <span className="inline-flex items-center gap-1.5">
        <IconFlask className="size-4 text-primary" stroke={1.75} aria-hidden />
        You&apos;re exploring a demo classroom with sample data. Sage and code Run are turned off.
      </span>
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
