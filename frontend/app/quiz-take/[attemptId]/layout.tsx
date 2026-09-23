"use client";

import { useAuthGuard } from "@/hooks/useAuthGuard";

// Deliberately outside app/student/ (see CLAUDE.md / the quiz feature plan): a timed attempt needs a
// layout with zero navigation chrome - no sidebar, no top nav, nothing to click away through while
// the clock is running. Needs its own useAuthGuard call since it doesn't inherit one from
// app/student/layout.tsx.
export default function QuizTakeLayout({ children }: { children: React.ReactNode }) {
  const authorized = useAuthGuard("STUDENT");
  if (!authorized) return null;

  return <div className="min-h-screen bg-background">{children}</div>;
}
