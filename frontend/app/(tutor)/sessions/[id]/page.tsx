"use client";

import { use } from "react";

import { SessionNoteForm } from "@/components/session/SessionNoteForm";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useCompleteSession, useSession, useSessionNotes } from "@/hooks/useSessions";
import type { SessionStatus } from "@/types/session";

const statusVariant: Record<SessionStatus, "default" | "secondary" | "outline"> = {
  SCHEDULED: "secondary",
  COMPLETED: "default",
  CANCELLED: "outline",
};

export default function SessionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const session = useSession(id);
  const notes = useSessionNotes(id);
  const completeSession = useCompleteSession();

  if (session.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  if (!session.data) {
    return <p className="text-sm text-muted-foreground">Session not found.</p>;
  }

  const canComplete = session.data.status === "SCHEDULED";

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-medium text-foreground">{session.data.subject}</h1>
            <Badge variant={statusVariant[session.data.status]}>{session.data.status}</Badge>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            {session.data.studentName} · {new Date(session.data.date).toLocaleString()} ·{" "}
            {session.data.duration} min
            {session.data.courseName ? ` · ${session.data.courseName}` : ""}
          </p>
        </div>
        {canComplete ? (
          <Button
            type="button"
            variant="outline"
            disabled={completeSession.isPending}
            onClick={() => completeSession.mutate(id)}
          >
            {completeSession.isPending ? "Completing..." : "Mark complete"}
          </Button>
        ) : null}
      </div>

      {notes.isLoading ? (
        <Skeleton className="h-32 w-full" />
      ) : (
        <SessionNoteForm sessionId={id} note={notes.data ?? null} />
      )}
    </div>
  );
}
