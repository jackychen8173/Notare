"use client";

import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { IconArrowsMaximize, IconArrowsMinimize, IconPlayerPlay } from "@tabler/icons-react";

import { CodeEditor } from "@/components/assignment/CodeEditor";
import { CodeOutputPanel } from "@/components/assignment/CodeOutputPanel";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { useRunCode } from "@/hooks/useCodeRun";
import { useSubmitAssignment } from "@/hooks/useSubmissions";
import type { CodeRunResult } from "@/types/codeRun";
import { DemoDisabledNote } from "@/components/layout/DemoDisabledNote";
import { useIsDemo } from "@/hooks/useIsDemo";
import { errorMessage } from "@/lib/api";
import { cn } from "@/lib/utils";

const submissionSchema = z.object({
  content: z.string().min(1, "Write your code before submitting"),
});

type SubmissionValues = z.infer<typeof submissionSchema>;

interface SubmissionFormProps {
  assignmentId: string;
  /** Shown beside the editor in full-screen mode. */
  instructions?: { title: string; description: string | null };
  /** Starting code, e.g. the previous version when resubmitting. */
  initialContent?: string;
  submitLabel?: string;
  onSubmitted?: () => void;
  onCancel?: () => void;
}

// Unsubmitted work is kept in this browser (per assignment) so a reload or a closed tab doesn't lose it.
// Best-effort only: storage can be unavailable (private windows), so every access is guarded.
const draftKey = (assignmentId: string) => `notare:draft:${assignmentId}`;

function readDraft(assignmentId: string): string | null {
  try {
    return window.localStorage.getItem(draftKey(assignmentId));
  } catch {
    return null;
  }
}

function writeDraft(assignmentId: string, content: string | null) {
  try {
    if (content) window.localStorage.setItem(draftKey(assignmentId), content);
    else window.localStorage.removeItem(draftKey(assignmentId));
  } catch {
    // Drafts are a convenience; ignore storage failures.
  }
}

export function SubmissionForm({
  assignmentId,
  instructions,
  initialContent = "",
  submitLabel = "Submit assignment",
  onSubmitted,
  onCancel,
}: SubmissionFormProps) {
  const submitAssignment = useSubmitAssignment(assignmentId);
  const runCode = useRunCode(assignmentId);
  const isDemo = useIsDemo();
  const [runResult, setRunResult] = useState<CodeRunResult | null>(null);
  const [fullScreen, setFullScreen] = useState(false);
  const {
    control,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<SubmissionValues>({
    resolver: zodResolver(submissionSchema),
    defaultValues: { content: initialContent },
  });
  const content = watch("content");

  // Restore a saved draft once on mount (after hydration, since storage is browser-only).
  useEffect(() => {
    const draft = readDraft(assignmentId);
    if (draft) setValue("content", draft);
  }, [assignmentId, setValue]);

  useEffect(() => {
    if (content !== initialContent) writeDraft(assignmentId, content);
  }, [assignmentId, content, initialContent]);

  // Esc leaves full screen; the page behind shouldn't scroll while it's open.
  useEffect(() => {
    if (!fullScreen) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setFullScreen(false);
    };
    window.addEventListener("keydown", onKeyDown);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = overflow;
    };
  }, [fullScreen]);

  function onSubmit(values: SubmissionValues) {
    submitAssignment.mutate(values.content, {
      onSuccess: () => {
        writeDraft(assignmentId, null);
        setFullScreen(false);
        onSubmitted?.();
      },
    });
  }

  function handleRun() {
    if (isDemo || runCode.isPending || !content?.trim()) return;
    setRunResult(null);
    runCode.mutate(content, { onSuccess: setRunResult });
  }

  const runButton = (
    <Button type="button" variant="outline" disabled={isDemo || runCode.isPending || !content?.trim()} onClick={handleRun}>
      <IconPlayerPlay />
      {runCode.isPending ? "Running..." : "Run"}
      <kbd className="ml-1 hidden font-mono text-[0.7rem] text-muted-foreground sm:inline">Ctrl+Enter</kbd>
    </Button>
  );
  const submitButton = (
    <Button type="submit" disabled={submitAssignment.isPending}>
      {submitAssignment.isPending ? "Submitting..." : submitLabel}
    </Button>
  );
  const output = (
    <>
      {isDemo ? <DemoDisabledNote feature="Running code" /> : null}
      {runCode.isError ? <p className="text-sm text-destructive">{errorMessage(runCode.error)}</p> : null}
      {runResult ? <CodeOutputPanel result={runResult} /> : null}
      {submitAssignment.isError ? (
        <p className="text-sm text-destructive">{errorMessage(submitAssignment.error)}</p>
      ) : null}
    </>
  );

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      onKeyDown={(event) => {
        if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
          event.preventDefault();
          handleRun();
        }
      }}
      className={cn(
        "flex flex-col gap-4",
        fullScreen && "fixed inset-0 z-50 gap-0 bg-background",
      )}
    >
      {fullScreen ? (
        <div className="flex flex-wrap items-center gap-2 border-b border-border px-4 py-2">
          <p className="min-w-0 flex-1 truncate font-mono text-sm text-foreground">
            {instructions?.title ?? "Your code"}
          </p>
          {runButton}
          {submitButton}
          <Button type="button" variant="ghost" onClick={() => setFullScreen(false)}>
            <IconArrowsMinimize /> Exit <kbd className="font-mono text-[0.7rem] text-muted-foreground">Esc</kbd>
          </Button>
        </div>
      ) : null}

      <div className={cn(fullScreen ? "grid min-h-0 flex-1 lg:grid-cols-[minmax(0,1fr)_24rem]" : "flex flex-col gap-1.5")}>
        <div className={cn(fullScreen ? "flex min-h-0 flex-col p-3" : "flex flex-col gap-1.5")}>
          {!fullScreen ? (
            <div className="flex items-center justify-between">
              <Label>Your code</Label>
              <Button type="button" size="sm" variant="ghost" onClick={() => setFullScreen(true)}>
                <IconArrowsMaximize /> Full screen
              </Button>
            </div>
          ) : null}
          <Controller
            control={control}
            name="content"
            render={({ field }) => (
              <CodeEditor
                value={field.value}
                onChange={field.onChange}
                height={fullScreen ? "100%" : "320px"}
                className={fullScreen ? "min-h-0 flex-1" : undefined}
              />
            )}
          />
          {errors.content ? <p className="text-xs text-destructive">{errors.content.message}</p> : null}
        </div>

        {fullScreen ? (
          <aside className="flex min-h-0 flex-col gap-4 overflow-y-auto border-t border-border p-4 lg:border-l lg:border-t-0">
            {instructions ? (
              <div>
                <p className="mb-1 font-mono text-xs uppercase tracking-wider text-muted-foreground">Instructions</p>
                <p className="whitespace-pre-wrap text-sm text-foreground">
                  {instructions.description || "No extra instructions."}
                </p>
              </div>
            ) : null}
            {output}
          </aside>
        ) : null}
      </div>

      {!fullScreen ? (
        <>
          <div className="flex gap-2">
            {runButton}
            {submitButton}
            {onCancel ? (
              <Button type="button" variant="ghost" onClick={onCancel}>
                Cancel
              </Button>
            ) : null}
          </div>
          {output}
        </>
      ) : null}
    </form>
  );
}
