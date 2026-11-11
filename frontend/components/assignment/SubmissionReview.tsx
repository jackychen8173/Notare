"use client";

import { useState } from "react";
import Link from "next/link";
import { IconGitCompare } from "@tabler/icons-react";

import { SubmissionFiles } from "@/components/file/FileList";
import { LateBadge } from "@/components/assignment/AssignmentTiming";
import { CodeReviewView } from "@/components/assignment/CodeReviewView";
import { SubmissionTestsCard } from "@/components/assignment/SubmissionTestsCard";
import { SageFeedbackBlock } from "@/components/sage/SageFeedbackBlock";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useRubric } from "@/hooks/useRubrics";
import {
  useLineCommentMutations,
  useLineComments,
  useReleaseFeedback,
  useReviewSubmission,
  useUpdateRubricScores,
} from "@/hooks/useSubmissions";
import type { FeedbackStatus, Submission } from "@/types/submission";
import { DemoDisabledNote } from "@/components/layout/DemoDisabledNote";
import { useIsDemo } from "@/hooks/useIsDemo";
import { errorMessage } from "@/lib/api";
import { cn } from "@/lib/utils";

const statusVariant: Record<FeedbackStatus, "default" | "secondary"> = {
  PENDING: "secondary",
  APPROVED: "default",
  REVISED: "default",
};

interface SubmissionReviewProps {
  submission: Submission;
  /** Every version this student submitted for the assignment (any order); drives the version tabs and diff. */
  versions: Submission[];
}

function RubricScoring({ submission }: { submission: Submission }) {
  const rubric = useRubric(submission.assignmentId);
  const updateScores = useUpdateRubricScores(submission.id);
  const [scores, setScores] = useState<Record<string, string>>(() =>
    Object.fromEntries(submission.rubricScores.map((s) => [s.criterionId, String(s.pointsAwarded)])),
  );

  if (rubric.isLoading) {
    return <Skeleton className="h-16 w-full" />;
  }

  if (!rubric.data) {
    return null;
  }

  function onSave() {
    if (!rubric.data) return;
    updateScores.mutate(
      rubric.data.criteria.map((criterion) => ({
        criterionId: criterion.id,
        pointsAwarded: Number(scores[criterion.id] ?? 0),
      })),
    );
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <p className="text-sm font-medium text-foreground">{rubric.data.title}</p>
        {rubric.data.criteria.map((criterion) => (
          <div key={criterion.id} className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm text-foreground">{criterion.name}</p>
              {criterion.description ? (
                <p className="text-xs text-muted-foreground">{criterion.description}</p>
              ) : null}
            </div>
            <div className="flex items-center gap-1.5">
              <Input
                type="number"
                min={0}
                max={criterion.pointsPossible}
                step="0.5"
                className="w-20"
                value={scores[criterion.id] ?? ""}
                onChange={(event) => setScores((prev) => ({ ...prev, [criterion.id]: event.target.value }))}
              />
              <span className="text-xs text-muted-foreground">/ {criterion.pointsPossible}</span>
            </div>
          </div>
        ))}
        <Button type="button" size="sm" className="self-end" disabled={updateScores.isPending} onClick={onSave}>
          {updateScores.isPending ? "Saving..." : "Save rubric scores"}
        </Button>
      </CardContent>
    </Card>
  );
}

export function SubmissionReview({ submission, versions }: SubmissionReviewProps) {
  const [tutorFeedback, setTutorFeedback] = useState(submission.tutorFeedback ?? "");
  const [grade, setGrade] = useState(submission.grade ?? "");
  const [points, setPoints] = useState(submission.pointsEarned != null ? String(submission.pointsEarned) : "");
  const scored = submission.pointsPossible != null;
  const pointsValue = points.trim() === "" ? undefined : Number(points);
  const pointsInvalid = pointsValue !== undefined && (!Number.isFinite(pointsValue) || pointsValue < 0);
  const afterPenalty =
    pointsValue !== undefined && !pointsInvalid && submission.latePenaltyPercent > 0
      ? Math.round(pointsValue * (100 - submission.latePenaltyPercent)) / 100
      : null;
  const [showChanges, setShowChanges] = useState(true);
  const reviewSubmission = useReviewSubmission(submission.id);
  const isDemo = useIsDemo();
  const releaseFeedback = useReleaseFeedback(submission.id);
  const lineComments = useLineComments(submission.id);
  const { add, update, remove } = useLineCommentMutations(submission.id);

  const isReleased = submission.releasedAt != null;
  const sortedVersions = [...versions].sort((a, b) => a.attemptNumber - b.attemptNumber);
  const previous = sortedVersions.filter((v) => v.attemptNumber < submission.attemptNumber).at(-1);
  const comments = lineComments.data ?? [];
  const pendingSuggestions = comments.filter((c) => c.status === "SUGGESTED").length;

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div className="flex min-w-0 flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          {sortedVersions.length > 1 ? (
            <nav aria-label="Versions" className="flex gap-1 font-mono text-xs">
              {sortedVersions.map((version) => (
                <Link
                  key={version.id}
                  href={`/submissions/${version.id}/review`}
                  className={cn(
                    "rounded-md border px-2 py-1",
                    version.id === submission.id
                      ? "border-primary bg-primary-soft text-primary"
                      : "border-border text-muted-foreground hover:bg-muted",
                  )}
                >
                  v{version.attemptNumber}
                </Link>
              ))}
            </nav>
          ) : (
            <span className="font-mono text-xs text-muted-foreground">v{submission.attemptNumber}</span>
          )}
          <div className="flex items-center gap-2">
            {previous ? (
              <Button size="sm" variant={showChanges ? "secondary" : "ghost"} onClick={() => setShowChanges((v) => !v)}>
                <IconGitCompare />
                {showChanges ? `Changes since v${previous.attemptNumber}` : "Show changes"}
              </Button>
            ) : null}
            {submission.late ? (
              <LateBadge daysLate={submission.daysLate} latePenaltyPercent={submission.latePenaltyPercent} />
            ) : null}
            <Badge variant={statusVariant[submission.feedbackStatus]}>
              {isReleased ? submission.feedbackStatus : "Not released"}
            </Badge>
          </div>
        </div>

        <CodeReviewView
          code={submission.content}
          comments={comments}
          compareTo={previous && showChanges ? previous.content : null}
          actions={{
            add: (lineNumber, body) => add.mutateAsync({ lineNumber, body }),
            update: (id, body) => update.mutateAsync({ id, body }),
            remove: (id) => remove.mutateAsync(id),
          }}
        />
        <p className="text-xs text-muted-foreground">
          Hover a line number and press <span className="font-mono">+</span> to comment on that line. Comments reach the
          student when you release feedback.
        </p>
        <SubmissionFiles submissionId={submission.id} />
      </div>

      <aside className="flex flex-col gap-4 lg:sticky lg:top-4 lg:self-start">
        {submission.sageFeedback ? (
          <>
            <SageFeedbackBlock feedbackJson={submission.sageFeedback} />
            {pendingSuggestions > 0 ? (
              <p className="text-xs text-sage-text">
                Sage left {pendingSuggestions} line {pendingSuggestions === 1 ? "suggestion" : "suggestions"}. Accept or
                dismiss each one in the code. Students never see a suggestion you haven&apos;t accepted.
              </p>
            ) : null}
          </>
        ) : (
          <div className="flex flex-col gap-1.5">
            <Button
              type="button"
              variant="outline"
              className="self-start"
              disabled={isDemo || reviewSubmission.isPending}
              onClick={() => reviewSubmission.mutate()}
            >
              {reviewSubmission.isPending ? "Asking Sage..." : "Get Sage feedback"}
            </Button>
            <p className="text-xs text-muted-foreground">Sage drafts overall feedback and comments on specific lines.</p>
            {isDemo ? <DemoDisabledNote /> : null}
            {reviewSubmission.isError ? (
              <p className="text-xs text-destructive">{errorMessage(reviewSubmission.error)}</p>
            ) : null}
          </div>
        )}

        <SubmissionTestsCard
          submission={submission}
          onUseScore={scored ? (value) => setPoints(String(value)) : undefined}
        />

        <RubricScoring key={submission.id} submission={submission} />

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="tutor-feedback">Overall feedback (optional)</Label>
            <Textarea
              id="tutor-feedback"
              rows={4}
              value={tutorFeedback}
              onChange={(event) => setTutorFeedback(event.target.value)}
              placeholder="Add or revise feedback before releasing to the student..."
            />
          </div>
          {scored ? (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="points">Score (optional)</Label>
              <div className="flex items-center gap-2">
                <Input
                  id="points"
                  inputMode="decimal"
                  value={points}
                  onChange={(event) => setPoints(event.target.value)}
                  className="max-w-24"
                  aria-invalid={pointsInvalid || undefined}
                />
                <span className="font-mono text-xs text-muted-foreground">/ {submission.pointsPossible}</span>
              </div>
              {pointsInvalid ? (
                <p className="text-xs text-destructive">Enter a number of points, 0 or more.</p>
              ) : afterPenalty != null ? (
                <p className="text-xs text-muted-foreground">
                  Handed in {submission.daysLate} {submission.daysLate === 1 ? "day" : "days"} late, so it counts as{" "}
                  <span className="font-mono">{afterPenalty}</span> after −{submission.latePenaltyPercent}%.
                </p>
              ) : null}
            </div>
          ) : null}
          {!scored || submission.grade ? (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="grade">{scored ? "Earlier grade" : "Grade (optional)"}</Label>
              <Input id="grade" value={grade} onChange={(event) => setGrade(event.target.value)} className="max-w-32" />
              {scored ? (
                <p className="text-xs text-muted-foreground">From before this assignment had points. Clear it once you give a score.</p>
              ) : null}
            </div>
          ) : null}
          <Button
            type="button"
            disabled={releaseFeedback.isPending || pointsInvalid}
            onClick={() =>
              releaseFeedback.mutate({
                tutorFeedback: tutorFeedback.trim() || undefined,
                grade: grade.trim() || undefined,
                pointsEarned: scored ? pointsValue : undefined,
              })
            }
          >
            {releaseFeedback.isPending ? "Releasing..." : isReleased ? "Update and re-release" : "Release to student"}
          </Button>
          {releaseFeedback.isError ? (
            <p className="text-xs text-destructive">{errorMessage(releaseFeedback.error)}</p>
          ) : null}
        </div>
      </aside>
    </div>
  );
}
