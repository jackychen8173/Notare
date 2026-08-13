"use client";

import { useState } from "react";

import { SageNotesDraft } from "@/components/sage/SageNotesDraft";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useDraftSessionNotes, useSaveSessionNotes } from "@/hooks/useSessions";
import type { SessionNote } from "@/types/session";

interface SessionNoteFormProps {
  sessionId: string;
  note: SessionNote | null;
}

export function SessionNoteForm({ sessionId, note }: SessionNoteFormProps) {
  // Lazy initializer only - the page gates rendering until `note` has resolved
  // from the query, so this always mounts with the real value. Saving/drafting
  // both keep rawNotes in sync with what the user already has locally, so no
  // effect is needed to resync from the note prop after mount.
  const [rawNotes, setRawNotes] = useState(note?.rawNotes ?? "");
  const saveNotes = useSaveSessionNotes(sessionId);
  const draftNotes = useDraftSessionNotes(sessionId);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="raw-notes">Raw notes</Label>
        <Textarea
          id="raw-notes"
          rows={6}
          value={rawNotes}
          onChange={(event) => setRawNotes(event.target.value)}
          placeholder="Jot down what happened in the session..."
        />
        <div className="flex gap-2">
          <Button
            type="button"
            size="sm"
            disabled={!rawNotes.trim() || saveNotes.isPending}
            onClick={() => saveNotes.mutate(rawNotes)}
          >
            {saveNotes.isPending ? "Saving..." : "Save notes"}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            disabled={!note?.rawNotes || draftNotes.isPending}
            onClick={() => draftNotes.mutate()}
          >
            {draftNotes.isPending ? "Drafting..." : "Draft with Sage"}
          </Button>
        </div>
      </div>

      {note?.formattedNotes ? <SageNotesDraft formattedNotes={note.formattedNotes} /> : null}
    </div>
  );
}
