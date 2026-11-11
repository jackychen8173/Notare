"use client";

import { useState } from "react";
import { IconFileText, IconKeyboard } from "@tabler/icons-react";

import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { DataFile } from "@/types/codeRun";
import { cn } from "@/lib/utils";

interface RunInputPanelProps {
  stdin: string;
  onStdinChange: (value: string) => void;
  /** The assignment's data files, if any; Run puts them next to the program. */
  dataFiles?: DataFile[];
}

/**
 * What a Run reads: the keyboard input (fed to System.in, so Scanner works) and, on an
 * assignment, the teacher's data files, which open by name (new File("scores.txt")).
 */
export function RunInputPanel({ stdin, onStdinChange, dataFiles = [] }: RunInputPanelProps) {
  const [openFile, setOpenFile] = useState<string | null>(null);
  const shown = dataFiles.find((file) => file.id === openFile) ?? null;

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="run-stdin" className="flex items-center gap-1.5">
          <IconKeyboard className="size-4 text-muted-foreground" /> Input
        </Label>
        <Textarea
          id="run-stdin"
          value={stdin}
          onChange={(event) => onStdinChange(event.target.value)}
          placeholder="What your program reads with Scanner, one answer per line"
          rows={3}
          spellCheck={false}
          className="font-mono text-xs"
        />
      </div>

      {dataFiles.length > 0 ? (
        <div className="flex flex-col gap-1.5">
          <p className="font-mono text-xs uppercase tracking-wider text-muted-foreground">Data files</p>
          <div className="flex flex-wrap gap-1.5">
            {dataFiles.map((file) => (
              <button
                key={file.id}
                type="button"
                onClick={() => setOpenFile(openFile === file.id ? null : file.id)}
                aria-pressed={openFile === file.id}
                className={cn(
                  "inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 font-mono text-xs text-foreground hover:bg-muted",
                  openFile === file.id && "bg-muted",
                )}
              >
                <IconFileText className="size-3.5 text-muted-foreground" />
                {file.name}
              </button>
            ))}
          </div>
          {shown ? (
            <pre className="max-h-48 overflow-auto rounded-md border border-border bg-muted/40 p-2 font-mono text-xs text-foreground">
              {shown.content || "(empty file)"}
            </pre>
          ) : (
            <p className="text-xs text-muted-foreground">Your program can open these by name, like new File(&quot;{dataFiles[0].name}&quot;).</p>
          )}
        </div>
      ) : null}
    </div>
  );
}
