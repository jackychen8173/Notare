"use client";

import { Fragment, useMemo, useState } from "react";
import { IconCheck, IconPencil, IconPlus, IconSparkles, IconTrash } from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { highlightJavaLines, type HighlightToken } from "@/lib/highlight";
import { diffLines } from "@/lib/lineDiff";
import { cn } from "@/lib/utils";
import type { LineComment } from "@/types/submission";

/** What the tutor can do to comments. Omit for a read-only (student) view. */
export interface LineCommentActions {
  add: (lineNumber: number, body: string) => Promise<unknown>;
  update: (id: string, body: string) => Promise<unknown>;
  remove: (id: string) => Promise<unknown>;
}

interface CodeReviewViewProps {
  code: string;
  comments: LineComment[];
  actions?: LineCommentActions;
  /** An earlier version's code: lines changed since then are tinted, removed ones shown struck through. */
  compareTo?: string | null;
  className?: string;
}

type Row =
  | { kind: "line"; lineNumber: number; tokens: HighlightToken[]; added: boolean }
  | { kind: "removed"; text: string };

export function CodeReviewView({ code, comments, actions, compareTo, className }: CodeReviewViewProps) {
  const [composingLine, setComposingLine] = useState<number | null>(null);

  const rows = useMemo<Row[]>(() => {
    const highlighted = highlightJavaLines(code);
    if (compareTo == null) {
      return highlighted.map((tokens, i) => ({ kind: "line", lineNumber: i + 1, tokens, added: false }));
    }
    return diffLines(compareTo, code).map((line): Row =>
      line.kind === "removed"
        ? { kind: "removed", text: line.text }
        : { kind: "line", lineNumber: line.newLine, tokens: highlighted[line.newLine - 1] ?? [], added: line.kind === "added" },
    );
  }, [code, compareTo]);

  const commentsByLine = useMemo(() => {
    const byLine = new Map<number, LineComment[]>();
    for (const comment of comments) {
      byLine.set(comment.lineNumber, [...(byLine.get(comment.lineNumber) ?? []), comment]);
    }
    return byLine;
  }, [comments]);

  return (
    <div
      className={cn(
        "overflow-hidden rounded-card border border-border bg-card font-mono text-[0.8rem] leading-6 shadow-card",
        className,
      )}
    >
      <div className="overflow-x-auto">
        <table className="w-full border-collapse">
          <tbody>
            {rows.map((row, index) => {
              if (row.kind === "removed") {
                return (
                  <tr key={`removed-${index}`} className="bg-[var(--diff-removed)]">
                    <td className="w-12 select-none pr-3 text-right align-top text-muted-foreground/60">−</td>
                    <td className="whitespace-pre pr-4 text-muted-foreground line-through decoration-destructive/40">
                      {row.text || " "}
                    </td>
                  </tr>
                );
              }

              const lineComments = commentsByLine.get(row.lineNumber) ?? [];
              const composing = composingLine === row.lineNumber;
              return (
                <Fragment key={`line-${row.lineNumber}-${index}`}>
                  <tr className={cn("group", row.added && "bg-[var(--diff-added)]")}>
                    <td className="relative w-12 select-none pr-3 text-right align-top text-muted-foreground/70">
                      {actions ? (
                        <button
                          type="button"
                          aria-label={`Comment on line ${row.lineNumber}`}
                          onClick={() => setComposingLine(row.lineNumber)}
                          className="absolute left-1 top-1 hidden size-4 items-center justify-center rounded bg-primary text-primary-foreground group-hover:flex focus-visible:flex"
                        >
                          <IconPlus className="size-3" />
                        </button>
                      ) : null}
                      {row.lineNumber}
                    </td>
                    <td className="whitespace-pre pr-4 text-foreground">
                      {row.tokens.length === 0
                        ? " "
                        : row.tokens.map((token, i) => (
                            <span key={i} className={token.className || undefined}>
                              {token.text}
                            </span>
                          ))}
                    </td>
                  </tr>
                  {lineComments.length > 0 || composing ? (
                    <tr>
                      <td />
                      <td className="py-1.5 pr-3 font-sans">
                        <div className="flex max-w-2xl flex-col gap-2">
                          {lineComments.map((comment) => (
                            <LineCommentCard key={comment.id} comment={comment} actions={actions} />
                          ))}
                          {composing && actions ? (
                            <CommentComposer
                              submitLabel="Comment"
                              onCancel={() => setComposingLine(null)}
                              onSubmit={async (body) => {
                                await actions.add(row.lineNumber, body);
                                setComposingLine(null);
                              }}
                            />
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  ) : null}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function LineCommentCard({ comment, actions }: { comment: LineComment; actions?: LineCommentActions }) {
  const [editing, setEditing] = useState(false);
  const suggestion = comment.status === "SUGGESTED";
  const fromSage = comment.source === "SAGE";

  if (editing && actions) {
    return (
      <CommentComposer
        initialBody={comment.body}
        submitLabel={suggestion ? "Accept edited" : "Save"}
        onCancel={() => setEditing(false)}
        onSubmit={async (body) => {
          await actions.update(comment.id, body);
          setEditing(false);
        }}
      />
    );
  }

  return (
    <div
      className={cn(
        "rounded-lg border px-3 py-2 text-sm",
        suggestion ? "border-dashed border-sage-border bg-sage-surface" : "border-border bg-background",
      )}
    >
      <div className="mb-0.5 flex items-center gap-1.5 text-xs">
        {fromSage ? (
          <span className="inline-flex items-center gap-1 font-medium text-sage-text">
            <IconSparkles className="size-3.5" />
            {suggestion ? "Sage suggestion" : "Sage"}
          </span>
        ) : (
          <span className="font-medium text-foreground">Teacher</span>
        )}
        <span className="font-mono text-muted-foreground">L{comment.lineNumber}</span>
        {suggestion ? <span className="text-sage-text/70">· only you can see this until you accept it</span> : null}
      </div>
      <p className={cn("whitespace-pre-wrap", suggestion ? "text-sage-text" : "text-foreground")}>{comment.body}</p>
      {actions ? (
        <div className="mt-1.5 flex gap-1">
          {suggestion ? (
            <Button size="xs" variant="outline" onClick={() => actions.update(comment.id, comment.body)}>
              <IconCheck /> Accept
            </Button>
          ) : null}
          <Button size="xs" variant="ghost" onClick={() => setEditing(true)}>
            <IconPencil /> Edit
          </Button>
          <Button size="xs" variant="ghost" onClick={() => actions.remove(comment.id)}>
            <IconTrash /> {suggestion ? "Dismiss" : "Delete"}
          </Button>
        </div>
      ) : null}
    </div>
  );
}

function CommentComposer({
  initialBody = "",
  submitLabel,
  onSubmit,
  onCancel,
}: {
  initialBody?: string;
  submitLabel: string;
  onSubmit: (body: string) => Promise<void>;
  onCancel: () => void;
}) {
  const [body, setBody] = useState(initialBody);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);

  async function submit() {
    if (!body.trim() || saving) return;
    setSaving(true);
    setError(false);
    try {
      await onSubmit(body.trim());
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex flex-col gap-1.5 rounded-lg border border-border bg-background p-2">
      <Textarea
        autoFocus
        rows={2}
        value={body}
        placeholder="Leave a comment on this line..."
        onChange={(event) => setBody(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter" && (event.metaKey || event.ctrlKey)) {
            event.preventDefault();
            void submit();
          } else if (event.key === "Escape") {
            event.stopPropagation();
            onCancel();
          }
        }}
      />
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs text-muted-foreground">
          <kbd className="font-mono">Ctrl</kbd>+<kbd className="font-mono">Enter</kbd> to save ·{" "}
          <kbd className="font-mono">Esc</kbd> to cancel
        </span>
        <div className="flex gap-1">
          <Button size="sm" variant="ghost" onClick={onCancel}>
            Cancel
          </Button>
          <Button size="sm" disabled={!body.trim() || saving} onClick={() => void submit()}>
            {saving ? "Saving..." : submitLabel}
          </Button>
        </div>
      </div>
      {error ? <p className="text-xs text-destructive">Couldn&apos;t save the comment. Try again.</p> : null}
    </div>
  );
}
