"use client";

import { useEffect, useState } from "react";
import { IconFileCode, IconPlayerPlay, IconPlus, IconTrash } from "@tabler/icons-react";

import { CodeEditor } from "@/components/assignment/CodeEditor";
import { CodeOutputPanel } from "@/components/assignment/CodeOutputPanel";
import { DemoDisabledNote } from "@/components/layout/DemoDisabledNote";
import { EmptyState } from "@/components/layout/EmptyState";
import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";
import { Kbd } from "@/components/ui/kbd";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useIsDemo } from "@/hooks/useIsDemo";
import { usePracticeFileMutations, usePracticeFiles, useRunPractice } from "@/hooks/useWorkspace";
import { errorMessage } from "@/lib/api";
import { timeAgo } from "@/lib/dates";
import { cn } from "@/lib/utils";
import type { CodeRunResult } from "@/types/codeRun";
import type { PracticeFile } from "@/types/workspace";

const STARTER = `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, AP CSA!");
    }
}
`;

const AUTOSAVE_MS = 800;

export default function WorkspacePage() {
  const files = usePracticeFiles();
  const { create, remove } = usePracticeFileMutations();
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const list = files.data ?? [];
  const selected = list.find((file) => file.id === selectedId) ?? list[0];

  function newFile() {
    const taken = new Set(list.map((file) => file.name));
    let n = 1;
    while (taken.has(`Practice${n}.java`)) n++;
    create.mutate({ name: `Practice${n}.java`, content: STARTER }, { onSuccess: (file) => setSelectedId(file.id) });
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Workspace"
        description="Your own practice files. Write Java, run it, and keep it for later. Only you can see these."
      />

      {files.isLoading ? (
        <Skeleton className="h-[28rem] w-full rounded-card" />
      ) : list.length === 0 ? (
        <EmptyState
          icon={IconFileCode}
          title="No practice files yet"
          description="Start a file to try out an idea, warm up before a quiz, or rework an example from class."
          action={
            <Button onClick={newFile} disabled={create.isPending}>
              <IconPlus /> New file
            </Button>
          }
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-[14rem_minmax(0,1fr)]">
          <aside className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <p className="font-mono text-xs uppercase tracking-wider text-muted-foreground">Files</p>
              <Button size="icon-sm" variant="ghost" aria-label="New file" onClick={newFile} disabled={create.isPending}>
                <IconPlus />
              </Button>
            </div>
            <ul className="flex gap-1 overflow-x-auto lg:flex-col lg:overflow-visible">
              {list.map((file) => (
                <li key={file.id}>
                  <button
                    type="button"
                    onClick={() => setSelectedId(file.id)}
                    className={cn(
                      "flex w-full items-center gap-2 whitespace-nowrap rounded-md px-2 py-1.5 text-left font-mono text-xs",
                      file.id === selected?.id
                        ? "bg-primary-soft text-primary"
                        : "text-muted-foreground hover:bg-muted hover:text-foreground",
                    )}
                  >
                    <IconFileCode className="size-4 shrink-0" stroke={1.75} />
                    <span className="truncate">{file.name}</span>
                  </button>
                </li>
              ))}
            </ul>
            {create.isError ? <p className="text-xs text-destructive">{errorMessage(create.error)}</p> : null}
          </aside>

          {selected ? (
            <FileEditor
              key={selected.id}
              file={selected}
              onDelete={() => {
                if (window.confirm(`Delete ${selected.name}? This can't be undone.`)) {
                  remove.mutate(selected.id, { onSuccess: () => setSelectedId(null) });
                }
              }}
            />
          ) : null}
        </div>
      )}
    </div>
  );
}

function FileEditor({ file, onDelete }: { file: PracticeFile; onDelete: () => void }) {
  const { save } = usePracticeFileMutations();
  const run = useRunPractice();
  const isDemo = useIsDemo();
  const [name, setName] = useState(file.name);
  const [content, setContent] = useState(file.content);
  const [result, setResult] = useState<CodeRunResult | null>(null);
  const [dirty, setDirty] = useState(false);
  const saveFile = save.mutate;

  // Autosave shortly after the last edit.
  useEffect(() => {
    if (!dirty || !name.trim()) return;
    const timer = window.setTimeout(() => {
      saveFile({ id: file.id, name: name.trim(), content }, { onSuccess: () => setDirty(false) });
    }, AUTOSAVE_MS);
    return () => window.clearTimeout(timer);
  }, [dirty, name, content, file.id, saveFile]);

  function handleRun() {
    if (isDemo || run.isPending || !content.trim()) return;
    setResult(null);
    run.mutate(content, { onSuccess: setResult });
  }

  const status = save.isPending ? "Saving..." : dirty ? "Unsaved" : `Saved ${timeAgo(file.updatedAt)}`;

  return (
    <div
      className="flex min-w-0 flex-col gap-3"
      onKeyDown={(event) => {
        if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
          event.preventDefault();
          handleRun();
        }
      }}
    >
      <div className="flex flex-wrap items-center gap-2">
        <Input
          aria-label="File name"
          value={name}
          maxLength={100}
          onChange={(event) => {
            setName(event.target.value);
            setDirty(true);
          }}
          className="h-8 w-56 font-mono text-xs"
        />
        <span className="font-mono text-xs text-muted-foreground" aria-live="polite">
          {status}
        </span>
        <div className="ml-auto flex items-center gap-2">
          <Button size="sm" variant="ghost" onClick={onDelete} aria-label="Delete file">
            <IconTrash />
          </Button>
          <Button size="sm" onClick={handleRun} disabled={isDemo || run.isPending || !content.trim()}>
            <IconPlayerPlay />
            {run.isPending ? "Running..." : "Run"}
            <span className="ml-1 hidden gap-0.5 sm:flex">
              <Kbd className="border-primary-foreground/30 bg-transparent text-primary-foreground/80">Ctrl</Kbd>
              <Kbd className="border-primary-foreground/30 bg-transparent text-primary-foreground/80">Enter</Kbd>
            </span>
          </Button>
        </div>
      </div>

      <CodeEditor
        value={content}
        onChange={(value) => {
          setContent(value);
          setDirty(true);
        }}
        height="min(60vh, 34rem)"
      />

      {isDemo ? <DemoDisabledNote feature="Running code" /> : null}
      {save.isError ? <p className="text-xs text-destructive">{errorMessage(save.error)}</p> : null}
      {run.isError ? <p className="text-sm text-destructive">{errorMessage(run.error)}</p> : null}
      {result ? <CodeOutputPanel result={result} /> : null}
    </div>
  );
}
