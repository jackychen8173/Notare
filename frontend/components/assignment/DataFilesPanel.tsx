"use client";

import { useRef, useState } from "react";
import { IconFileText, IconPencil, IconPlus, IconTrash, IconUpload } from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useDataFileMutations, useDataFiles } from "@/hooks/useDataFiles";
import { errorMessage } from "@/lib/api";
import type { DataFile } from "@/types/codeRun";

const MAX_FILES = 5;
const MAX_CHARS = 200_000;

type Draft = { id: string | null; name: string; content: string };

/**
 * Text files the students' programs can read when they press Run (a CSV of scores, a word
 * list). Students see them on the assignment once it opens.
 */
export function DataFilesPanel({ assignmentId }: { assignmentId: string }) {
  const files = useDataFiles(assignmentId);
  const { create, update, remove } = useDataFileMutations(assignmentId);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const picker = useRef<HTMLInputElement>(null);

  const list = files.data ?? [];
  const saving = create.isPending || update.isPending;
  const saveError = create.error ?? update.error;

  function startEdit(file: DataFile | null) {
    create.reset();
    update.reset();
    setLoadError(null);
    setDraft(file ? { id: file.id, name: file.name, content: file.content } : { id: null, name: "", content: "" });
  }

  async function loadFromComputer(file: File | undefined) {
    if (!file || !draft) return;
    const text = await file.text();
    if (text.length > MAX_CHARS) {
      setLoadError(`That file is too big (over ${MAX_CHARS.toLocaleString()} characters).`);
      return;
    }
    setLoadError(null);
    setDraft({ ...draft, content: text, name: draft.name || file.name });
  }

  function save() {
    if (!draft) return;
    const body = { name: draft.name.trim(), content: draft.content };
    const done = { onSuccess: () => setDraft(null) };
    if (draft.id) update.mutate({ id: draft.id, ...body }, done);
    else create.mutate(body, done);
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div>
            <h2 className="text-sm font-medium text-foreground">Data files</h2>
            <p className="text-xs text-muted-foreground">
              Text files your students&apos; programs can open by name when they press Run, like
              new File(&quot;scores.txt&quot;).
            </p>
          </div>
          {!draft && list.length < MAX_FILES ? (
            <Button size="sm" variant="outline" onClick={() => startEdit(null)}>
              <IconPlus /> Add data file
            </Button>
          ) : null}
        </div>

        {list.length > 0 ? (
          <ul className="flex flex-col divide-y divide-border rounded-md border border-border">
            {list.map((file) => (
              <li key={file.id} className="flex items-center gap-2 px-3 py-2">
                <IconFileText className="size-4 shrink-0 text-muted-foreground" />
                <span className="min-w-0 flex-1 truncate font-mono text-sm text-foreground">{file.name}</span>
                <span className="font-mono text-xs text-muted-foreground">
                  {file.content.split("\n").length} lines
                </span>
                <>
                    <Button size="sm" variant="ghost" aria-label={`Edit ${file.name}`} onClick={() => startEdit(file)}>
                      <IconPencil />
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label={`Delete ${file.name}`}
                      disabled={remove.isPending}
                      onClick={() => {
                        if (window.confirm(`Delete ${file.name}? Students' programs won't be able to read it.`)) {
                          remove.mutate(file.id);
                        }
                      }}
                    >
                      <IconTrash />
                    </Button>
                </>
              </li>
            ))}
          </ul>
        ) : !draft ? (
          <p className="text-xs text-muted-foreground">No data files.</p>
        ) : null}
        {remove.isError ? <p className="text-xs text-destructive">{errorMessage(remove.error)}</p> : null}

        {draft ? (
          <div className="flex flex-col gap-3 rounded-md border border-border p-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="data-file-name">File name</Label>
              <Input
                id="data-file-name"
                value={draft.name}
                maxLength={100}
                placeholder="scores.txt"
                onChange={(event) => setDraft({ ...draft, name: event.target.value })}
                className="w-64 font-mono text-sm"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <Label htmlFor="data-file-content">Contents</Label>
                <Button size="sm" variant="ghost" type="button" onClick={() => picker.current?.click()}>
                  <IconUpload /> Load from your computer
                </Button>
                <input
                  ref={picker}
                  type="file"
                  accept=".txt,.csv,.tsv,.dat,.json,text/*"
                  className="hidden"
                  onChange={(event) => {
                    void loadFromComputer(event.target.files?.[0]);
                    event.target.value = "";
                  }}
                />
              </div>
              <Textarea
                id="data-file-content"
                value={draft.content}
                rows={8}
                spellCheck={false}
                maxLength={MAX_CHARS}
                onChange={(event) => setDraft({ ...draft, content: event.target.value })}
                className="font-mono text-xs"
              />
            </div>
            {loadError ? <p className="text-xs text-destructive">{loadError}</p> : null}
            {saveError ? <p className="text-xs text-destructive">{errorMessage(saveError)}</p> : null}
            <div className="flex gap-2">
              <Button size="sm" onClick={save} disabled={saving || !draft.name.trim()}>
                {saving ? "Saving..." : "Save"}
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setDraft(null)}>
                Cancel
              </Button>
            </div>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
