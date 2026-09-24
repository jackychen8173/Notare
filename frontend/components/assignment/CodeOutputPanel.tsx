import { Card, CardContent } from "@/components/ui/card";
import type { CodeRunResult } from "@/types/codeRun";

interface CodeOutputPanelProps {
  result: CodeRunResult;
}

export function CodeOutputPanel({ result }: CodeOutputPanelProps) {
  if (result.compileError) {
    return (
      <Card>
        <CardContent className="flex flex-col gap-1.5">
          <p className="text-xs font-medium text-destructive">Compile error</p>
          <pre className="overflow-x-auto whitespace-pre-wrap font-mono text-xs text-destructive">
            {result.compileError}
          </pre>
        </CardContent>
      </Card>
    );
  }

  if (result.timedOut) {
    return (
      <Card>
        <CardContent>
          <p className="text-sm text-destructive">
            Your program didn&apos;t finish in time (30s limit) — check for an infinite loop.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-2">
        <div className="flex items-center justify-between">
          <p className="text-xs font-medium text-muted-foreground">Output</p>
          <p className="text-xs text-muted-foreground">Exit code: {result.exitCode}</p>
        </div>
        {result.stdout ? (
          <pre className="overflow-x-auto whitespace-pre-wrap font-mono text-xs text-foreground">
            {result.stdout}
          </pre>
        ) : (
          <p className="text-xs text-muted-foreground">(no output)</p>
        )}
        {result.stderr ? (
          <pre className="overflow-x-auto whitespace-pre-wrap font-mono text-xs text-destructive">
            {result.stderr}
          </pre>
        ) : null}
      </CardContent>
    </Card>
  );
}
