"use client";

import { useRef, useState } from "react";
import { IconDownload, IconFile, IconPaperclip, IconX } from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import { formatBytes, MAX_FILE_BYTES, openFile, useSubmissionFiles } from "@/hooks/useFiles";
import { errorMessage } from "@/lib/api";
import type { StoredFileInfo } from "@/types/file";

/** Files with an open button each, and a remove button when onRemove is given. */
export function FileList({
  files,
  onRemove,
  removingId,
}: {
  files: StoredFileInfo[];
  onRemove?: (id: string) => void;
  removingId?: string | null;
}) {
  const [error, setError] = useState<string | null>(null);
  if (files.length === 0) return null;

  return (
    <div className="flex flex-col gap-1">
      <ul className="flex flex-col divide-y divide-border rounded-lg border border-border">
        {files.map((file) => (
          <li key={file.id} className="flex items-center gap-2.5 px-3 py-2">
            <IconFile className="size-4 shrink-0 text-muted-foreground" stroke={1.75} aria-hidden />
            <button
              type="button"
              className="min-w-0 flex-1 truncate text-left text-sm text-foreground hover:underline"
              onClick={() => openFile(file.id).catch((e) => setError(errorMessage(e)))}
            >
              {file.fileName}
            </button>
            <span className="shrink-0 font-mono text-[11px] text-muted-foreground">{formatBytes(file.sizeBytes)}</span>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              aria-label={`Open ${file.fileName}`}
              onClick={() => openFile(file.id).catch((e) => setError(errorMessage(e)))}
            >
              <IconDownload className="size-4" />
            </Button>
            {onRemove ? (
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                aria-label={`Remove ${file.fileName}`}
                disabled={removingId === file.id}
                onClick={() => onRemove(file.id)}
              >
                <IconX className="size-4" />
              </Button>
            ) : null}
          </li>
        ))}
      </ul>
      {error ? <p className="text-xs text-destructive">{error}</p> : null}
    </div>
  );
}

/** A button that picks one file, checking the size limit before anything uploads. */
export function FilePickButton({
  label = "Attach a file",
  disabled,
  pending,
  onPick,
}: {
  label?: string;
  disabled?: boolean;
  pending?: boolean;
  onPick: (file: File) => void;
}) {
  const input = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);

  return (
    <div className="flex flex-col gap-1">
      <input
        ref={input}
        type="file"
        className="sr-only"
        aria-label={label}
        tabIndex={-1}
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.target.value = "";
          if (!file) return;
          if (file.size > MAX_FILE_BYTES) {
            setError("Files can be up to 10 MB");
            return;
          }
          setError(null);
          onPick(file);
        }}
      />
      <Button type="button" variant="outline" size="sm" className="self-start" disabled={disabled || pending} onClick={() => input.current?.click()}>
        <IconPaperclip className="size-4" />
        {pending ? "Uploading…" : label}
      </Button>
      {error ? <p className="text-xs text-destructive">{error}</p> : null}
    </div>
  );
}

/** The files handed in with a submission, if any. */
export function SubmissionFiles({ submissionId }: { submissionId: string }) {
  const files = useSubmissionFiles(submissionId);
  if (!files.data || files.data.length === 0) return null;
  return (
    <div className="flex flex-col gap-1.5">
      <p className="font-mono text-xs uppercase tracking-wider text-muted-foreground">
        Attached files · {files.data.length}
      </p>
      <FileList files={files.data} />
    </div>
  );
}
