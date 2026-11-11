"use client";

import { useState } from "react";
import { IconEyeOff, IconPencil, IconPlus, IconTrash } from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useAssignmentTests, useTestMutations } from "@/hooks/useAssignmentTests";
import { errorMessage } from "@/lib/api";
import type { AssignmentTest } from "@/types/codeRun";

const MAX_TESTS = 20;

type Draft = { id: string | null; name: string; stdin: string; expectedOutput: string; points: string; hidden: boolean };

const emptyDraft = (n: number): Draft => ({
  id: null,
  name: `Test ${n}`,
  stdin: "",
  expectedOutput: "",
  points: "1",
  hidden: false,
});

/**
 * Output checks for an assignment: run the student's program with an input and compare what it
 * prints. Students can check their code against these while they work; on the review page they
 * suggest a score. Hidden tests show students only pass or fail.
 */
export function TestsPanel({ assignmentId }: { assignmentId: string }) {
  const tests = useAssignmentTests(assignmentId);
  const { create, update, remove } = useTestMutations(assignmentId);
  const [draft, setDraft] = useState<Draft | null>(null);

  const list = tests.data ?? [];
  const saving = create.isPending || update.isPending;
  const saveError = create.error ?? update.error;
  const points = draft ? Number(draft.points) : 0;
  const pointsInvalid = draft !== null && (!Number.isInteger(points) || points < 0 || points > 100);
  const totalPoints = list.reduce((sum, test) => sum + test.points, 0);

  function startEdit(test: AssignmentTest | null) {
    create.reset();
    update.reset();
    setDraft(
      test
        ? {
            id: test.id,
            name: test.name,
            stdin: test.stdin ?? "",
            expectedOutput: test.expectedOutput ?? "",
            points: String(test.points),
            hidden: test.hidden,
          }
        : emptyDraft(list.length + 1),
    );
  }

  function save() {
    if (!draft || pointsInvalid) return;
    const body = {
      name: draft.name.trim(),
      stdin: draft.stdin,
      expectedOutput: draft.expectedOutput,
      points,
      hidden: draft.hidden,
    };
    const done = { onSuccess: () => setDraft(null) };
    if (draft.id) update.mutate({ id: draft.id, ...body }, done);
    else create.mutate(body, done);
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div>
            <h2 className="text-sm font-medium text-foreground">Tests</h2>
            <p className="text-xs text-muted-foreground">
              Each test runs the student&apos;s program with an input and checks what it prints. Students can check their
              code before submitting; on the review page the results suggest a score.
            </p>
          </div>
          {!draft && list.length < MAX_TESTS ? (
            <Button size="sm" variant="outline" onClick={() => startEdit(null)}>
              <IconPlus /> Add test
            </Button>
          ) : null}
        </div>

        {list.length > 0 ? (
          <ul className="flex flex-col divide-y divide-border rounded-md border border-border">
            {list.map((test) => (
              <li key={test.id} className="flex items-center gap-2 px-3 py-2">
                <span className="min-w-0 flex-1 truncate text-sm text-foreground">{test.name}</span>
                {test.hidden ? (
                  <span className="inline-flex items-center gap-1 font-mono text-[11px] text-muted-foreground">
                    <IconEyeOff className="size-3.5" /> hidden
                  </span>
                ) : null}
                <span className="font-mono text-xs text-muted-foreground">{test.points} pts</span>
                <Button size="sm" variant="ghost" aria-label={`Edit ${test.name}`} onClick={() => startEdit(test)}>
                  <IconPencil />
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  aria-label={`Delete ${test.name}`}
                  disabled={remove.isPending}
                  onClick={() => {
                    if (window.confirm(`Delete the test "${test.name}"?`)) remove.mutate(test.id);
                  }}
                >
                  <IconTrash />
                </Button>
              </li>
            ))}
            <li className="px-3 py-1.5 text-right font-mono text-xs text-muted-foreground">{totalPoints} pts total</li>
          </ul>
        ) : !draft ? (
          <p className="text-xs text-muted-foreground">No tests.</p>
        ) : null}
        {remove.isError ? <p className="text-xs text-destructive">{errorMessage(remove.error)}</p> : null}

        {draft ? (
          <div className="flex flex-col gap-3 rounded-md border border-border p-3">
            <div className="flex flex-wrap gap-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="test-name">Name</Label>
                <Input
                  id="test-name"
                  value={draft.name}
                  maxLength={100}
                  onChange={(event) => setDraft({ ...draft, name: event.target.value })}
                  className="w-64"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="test-points">Points</Label>
                <Input
                  id="test-points"
                  inputMode="numeric"
                  value={draft.points}
                  onChange={(event) => setDraft({ ...draft, points: event.target.value })}
                  className="w-20"
                  aria-invalid={pointsInvalid || undefined}
                />
              </div>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="test-stdin">Input</Label>
                <Textarea
                  id="test-stdin"
                  value={draft.stdin}
                  rows={5}
                  spellCheck={false}
                  placeholder="What the program reads, one answer per line (leave empty if it reads nothing)"
                  onChange={(event) => setDraft({ ...draft, stdin: event.target.value })}
                  className="font-mono text-xs"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="test-expected">Expected output</Label>
                <Textarea
                  id="test-expected"
                  value={draft.expectedOutput}
                  rows={5}
                  spellCheck={false}
                  placeholder="Exactly what the program should print"
                  onChange={(event) => setDraft({ ...draft, expectedOutput: event.target.value })}
                  className="font-mono text-xs"
                />
              </div>
            </div>
            <p className="text-xs text-muted-foreground">
              Spaces at the ends of lines, blank lines at the end and Windows line endings don&apos;t count. Everything else,
              including capital letters, has to match.
            </p>
            <label className="flex items-start gap-2 text-sm text-foreground">
              <input
                type="checkbox"
                className="mt-1 accent-primary"
                checked={draft.hidden}
                onChange={(event) => setDraft({ ...draft, hidden: event.target.checked })}
              />
              <span>
                Hidden
                <span className="block text-xs text-muted-foreground">
                  Students see only whether it passed, not the input or expected output.
                </span>
              </span>
            </label>
            {pointsInvalid ? <p className="text-xs text-destructive">Points must be a whole number from 0 to 100.</p> : null}
            {saveError ? <p className="text-xs text-destructive">{errorMessage(saveError)}</p> : null}
            <div className="flex gap-2">
              <Button size="sm" onClick={save} disabled={saving || pointsInvalid || !draft.name.trim()}>
                {saving ? "Saving..." : "Save test"}
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
