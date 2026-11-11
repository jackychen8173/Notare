"use client";

import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { IconArrowsMaximize, IconArrowsMinimize, IconChecklist, IconPlayerPlay } from "@tabler/icons-react";

import { CodeEditor } from "@/components/assignment/CodeEditor";
import { CodeOutputPanel } from "@/components/assignment/CodeOutputPanel";
import { RunInputPanel } from "@/components/assignment/RunInputPanel";
import { TestResultsList } from "@/components/assignment/TestResultsList";
import { FileList, FilePickButton } from "@/components/file/FileList";
import { Button } from "@/components/ui/button";
import { Kbd } from "@/components/ui/kbd";
import { Label } from "@/components/ui/label";
import { useRunCode } from "@/hooks/useCodeRun";
import { useStudentDataFiles } from "@/hooks/useDataFiles";
import { useCheckCode, useStudentTests } from "@/hooks/useAssignmentTests";
import { useDeleteDraftFile, useDraftFiles, useRefreshFilesAfterSubmit, useUploadDraftFile, useUploadsEnabled } from "@/hooks/useFiles";
import { useSubmitAssignment } from "@/hooks/useSubmissions";
import type { CodeRunResult, TestRunResult } from "@/types/codeRun";
import { DemoDisabledNote } from "@/components/layout/DemoDisabledNote";
import { useIsDemo } from "@/hooks/useIsDemo";
import { errorMessage } from "@/lib/api";
import { cn } from "@/lib/utils";

const submissionSchema = z.object({
  // May be empty when files are attached (checked in onSubmit).
  content: z.string(),
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
  const dataFiles = useStudentDataFiles(assignmentId);
  const [stdin, setStdin] = useState("");
  const tests = useStudentTests(assignmentId);
  const checkCode = useCheckCode(assignmentId);
  const [checkResult, setCheckResult] = useState<TestRunResult | null>(null);
  const hasTests = (tests.data?.length ?? 0) > 0;
  const drafts = useDraftFiles(assignmentId);
  const uploadDraft = useUploadDraftFile(assignmentId);
  const deleteDraft = useDeleteDraftFile(assignmentId);
  const refreshFiles = useRefreshFilesAfterSubmit(assignmentId);
  const uploadsEnabled = useUploadsEnabled();
  const [contentError, setContentError] = useState<string | null>(null);
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
    if (!values.content.trim() && (drafts.data?.length ?? 0) === 0) {
      setContentError("Write your code or attach a file before submitting");
      return;
    }
    setContentError(null);
    submitAssignment.mutate(values.content, {
      onSuccess: () => {
        refreshFiles();
        writeDraft(assignmentId, null);
        setFullScreen(false);
        onSubmitted?.();
      },
    });
  }

  function handleRun() {
    if (isDemo || runCode.isPending || !content?.trim()) return;
    setRunResult(null);
    runCode.mutate({ code: content, stdin }, { onSuccess: setRunResult });
  }

  const runButton = (
    <Button type="button" variant="outline" disabled={isDemo || runCode.isPending || !content?.trim()} onClick={handleRun}>
      <IconPlayerPlay />
      {runCode.isPending ? "Running..." : "Run"}
      <span className="ml-1 hidden gap-0.5 sm:flex">
        <Kbd>Ctrl</Kbd>
        <Kbd>Enter</Kbd>
      </span>
    </Button>
  );
  function handleCheck() {
    if (isDemo || checkCode.isPending || !content?.trim()) return;
    setCheckResult(null);
    checkCode.mutate(content, { onSuccess: setCheckResult });
  }

  const checkButton = hasTests ? (
    <Button type="button" variant="outline" disabled={isDemo || checkCode.isPending || !content?.trim()} onClick={handleCheck}>
      <IconChecklist />
      {checkCode.isPending ? "Checking..." : `Check (${tests.data?.length} ${tests.data?.length === 1 ? "test" : "tests"})`}
    </Button>
  ) : null;
  const submitButton = (
    <Button type="submit" disabled={submitAssignment.isPending}>
      {submitAssignment.isPending ? "Submitting..." : submitLabel}
    </Button>
  );
  const runInput = <RunInputPanel stdin={stdin} onStdinChange={setStdin} dataFiles={dataFiles.data} />;
  const output = (
    <>
      {isDemo ? <DemoDisabledNote feature="Running code" /> : null}
      {runCode.isError ? <p className="text-sm text-destructive">{errorMessage(runCode.error)}</p> : null}
      {runResult ? <CodeOutputPanel result={runResult} /> : null}
      {checkCode.isError ? <p className="text-sm text-destructive">{errorMessage(checkCode.error)}</p> : null}
      {checkResult ? <TestResultsList run={checkResult} /> : null}
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
          {checkButton}
          {submitButton}
          <Button type="button" variant="ghost" onClick={() => setFullScreen(false)}>
            <IconArrowsMinimize /> Exit <Kbd>Esc</Kbd>
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
          {contentError ? <p className="text-xs text-destructive">{contentError}</p> : null}
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
            {runInput}
            {output}
          </aside>
        ) : null}
      </div>

      {!fullScreen ? (
        <>
          {runInput}
          {uploadsEnabled || (drafts.data?.length ?? 0) > 0 ? (
            <div className="flex flex-col gap-2">
              <Label>Files (optional)</Label>
              <FileList
                files={drafts.data ?? []}
                onRemove={(id) => deleteDraft.mutate(id)}
                removingId={deleteDraft.isPending ? deleteDraft.variables : null}
              />
              {isDemo ? (
                <DemoDisabledNote feature="Attaching files" />
              ) : (
                <FilePickButton pending={uploadDraft.isPending} onPick={(file) => uploadDraft.mutate(file)} />
              )}
              {uploadDraft.isError ? <p className="text-xs text-destructive">{errorMessage(uploadDraft.error)}</p> : null}
              <p className="text-xs text-muted-foreground">Up to 10 files, 10 MB each. They go in with this submission.</p>
            </div>
          ) : null}
          <div className="flex flex-wrap gap-2">
            {runButton}
            {checkButton}
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
