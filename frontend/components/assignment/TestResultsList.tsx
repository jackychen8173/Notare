"use client";

import { IconCheck, IconEyeOff, IconX } from "@tabler/icons-react";

import type { TestRunResult } from "@/types/codeRun";
import { cn } from "@/lib/utils";

function Block({ label, text }: { label: string; text: string | null }) {
  return (
    <div className="flex min-w-0 flex-col gap-1">
      <p className="font-mono text-[11px] uppercase tracking-wider text-muted-foreground">{label}</p>
      <pre className="max-h-40 overflow-auto rounded-md border border-border bg-muted/40 p-2 font-mono text-xs text-foreground">
        {text ? text : <span className="text-muted-foreground">(nothing)</span>}
      </pre>
    </div>
  );
}

/**
 * Which tests passed. A failing visible test shows its input, the expected output and what the
 * program printed; a hidden test shows only pass or fail (for students its details never arrive).
 */
export function TestResultsList({ run }: { run: TestRunResult }) {
  const passed = run.results.filter((result) => result.passed).length;

  return (
    <div className="flex flex-col gap-2">
      <p className="text-sm font-medium text-foreground">
        {passed} of {run.results.length} {run.results.length === 1 ? "test passes" : "tests pass"}
        <span className="ml-2 font-mono text-xs font-normal text-muted-foreground">
          {run.pointsEarned}/{run.pointsPossible} pts
        </span>
      </p>
      {run.compileError ? (
        <pre className="overflow-x-auto whitespace-pre-wrap rounded-md border border-border p-2 font-mono text-xs text-destructive">
          {run.compileError}
        </pre>
      ) : null}
      <ul className="flex flex-col divide-y divide-border rounded-md border border-border">
        {run.results.map((result) => (
          <li key={result.testId} className="flex flex-col gap-2 px-3 py-2">
            <div className="flex items-center gap-2">
              {result.passed ? (
                <IconCheck className="size-4 shrink-0 text-primary" aria-label="Passed" />
              ) : (
                <IconX className="size-4 shrink-0 text-destructive" aria-label="Failed" />
              )}
              <span className={cn("min-w-0 flex-1 truncate text-sm", result.passed ? "text-foreground" : "text-destructive")}>
                {result.name}
              </span>
              {result.hidden ? (
                <span className="inline-flex items-center gap-1 font-mono text-[11px] text-muted-foreground">
                  <IconEyeOff className="size-3.5" /> hidden
                </span>
              ) : null}
              <span className="font-mono text-xs text-muted-foreground">{result.points} pts</span>
            </div>
            {!result.passed && result.timedOut ? (
              <p className="text-xs text-destructive">Didn&apos;t finish in time.</p>
            ) : null}
            {!result.passed && !run.compileError && result.expectedOutput !== null ? (
              <div className="grid gap-2 sm:grid-cols-3">
                <Block label="Input" text={result.stdin} />
                <Block label="Expected" text={result.expectedOutput} />
                <Block label="Got" text={result.actualOutput} />
              </div>
            ) : null}
            {!result.passed && result.stderr ? (
              <pre className="max-h-32 overflow-auto font-mono text-xs text-destructive">{result.stderr}</pre>
            ) : null}
          </li>
        ))}
      </ul>
    </div>
  );
}
