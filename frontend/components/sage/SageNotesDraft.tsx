interface SageNotesDraftProps {
  formattedNotes: string;
}

export function SageNotesDraft({ formattedNotes }: SageNotesDraftProps) {
  return (
    <div className="rounded-card border-hairline border-sage-border bg-sage-surface p-4">
      <p className="mb-1 text-xs font-medium text-sage-text">Sage draft</p>
      <p className="whitespace-pre-wrap text-sm text-sage-text">{formattedNotes}</p>
    </div>
  );
}
