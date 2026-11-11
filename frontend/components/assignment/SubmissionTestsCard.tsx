"use client";

import { IconChecklist } from "@tabler/icons-react";

import { TestResultsList } from "@/components/assignment/TestResultsList";
import { DemoDisabledNote } from "@/components/layout/DemoDisabledNote";
import { Button } from "@/components/ui/button";
import { useAssignmentTests, useRunSubmissionTests, useSubmissionTestResults } from "@/hooks/useAssignmentTests";
import { useIsDemo } from "@/hooks/useIsDemo";
import { errorMessage } from "@/lib/api";
import { timeAgo } from "@/lib/dates";
import type { Submission } from "@/types/submission";

interface SubmissionTestsCardProps {
  submission: Submission;
  /** Puts the suggested score in the score box; the teacher still releases it. */
  onUseScore?: (points: number) => void;
}

/**
 * The assignment's tests on this version, for the teacher. The run is stored on the submission
 * and suggests a score; students never see this run (they see the score once it's released).
 */
export function SubmissionTestsCard({ submission, onUseScore }: SubmissionTestsCardProps) {
  const tests = useAssignmentTests(submission.assignmentId);
  const hasTests = (tests.data?.length ?? 0) > 0;
  const stored = useSubmissionTestResults(submission.id, hasTests);
  const runTests = useRunSubmissionTests(submission.id);
  const isDemo = useIsDemo();

  if (!hasTests) return null;
  const run = stored.data ?? null;

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-2">
        <p className="flex items-center gap-1.5 font-mono text-xs uppercase tracking-wider text-muted-foreground">
          <IconChecklist className="size-4" /> Tests
        </p>
        <Button
          size="sm"
          variant="outline"
          disabled={isDemo || runTests.isPending || !submission.content?.trim()}
          onClick={() => runTests.mutate()}
        >
          {runTests.isPending ? "Running..." : run ? "Run again" : "Run tests"}
        </Button>
      </div>
      {isDemo ? <DemoDisabledNote feature="Running tests" /> : null}
      {runTests.isError ? <p className="text-xs text-destructive">{errorMessage(runTests.error)}</p> : null}
      {run ? (
        <>
          <TestResultsList run={run} />
          {run.ranAt ? <p className="font-mono text-xs text-muted-foreground">Ran {timeAgo(run.ranAt)}</p> : null}
          {run.suggestedScore != null && onUseScore ? (
            <Button
              size="sm"
              variant="secondary"
              className="self-start"
              onClick={() => onUseScore(run.suggestedScore as number)}
            >
              Use {run.suggestedScore} / {submission.pointsPossible} as the score
            </Button>
          ) : null}
        </>
      ) : !stored.isLoading ? (
        <p className="text-xs text-muted-foreground">Not run on this version yet.</p>
      ) : null}
    </div>
  );
}
