"use client";

import { useState } from "react";

import { SageFeedbackBlock } from "@/components/sage/SageFeedbackBlock";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useRubric } from "@/hooks/useRubrics";
import { useReleaseFeedback, useReviewSubmission, useUpdateRubricScores } from "@/hooks/useSubmissions";
import type { FeedbackStatus, Submission } from "@/types/submission";

const statusVariant: Record<FeedbackStatus, "default" | "secondary"> = {
  PENDING: "secondary",
  APPROVED: "default",
  REVISED: "default",
};

interface SubmissionReviewProps {
  submission: Submission;
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

export function SubmissionReview({ submission }: SubmissionReviewProps) {
  const [tutorFeedback, setTutorFeedback] = useState(submission.tutorFeedback ?? "");
  const [grade, setGrade] = useState(submission.grade ?? "");
  const reviewSubmission = useReviewSubmission(submission.id);
  const releaseFeedback = useReleaseFeedback(submission.id);

  const isReleased = submission.releasedAt != null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-muted-foreground">Submitted by</p>
          <p className="font-medium text-foreground">{submission.studentName}</p>
        </div>
        <Badge variant={statusVariant[submission.feedbackStatus]}>
          {submission.feedbackStatus}
        </Badge>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label>Submitted code</Label>
        <pre className="overflow-x-auto rounded-card border-hairline border-border bg-muted/40 p-4 font-mono text-xs text-foreground">
          {submission.content}
        </pre>
      </div>

      <RubricScoring submission={submission} />

      {submission.sageFeedback ? (
        <SageFeedbackBlock feedbackJson={submission.sageFeedback} />
      ) : (
        <Button
          type="button"
          variant="outline"
          disabled={reviewSubmission.isPending}
          onClick={() => reviewSubmission.mutate()}
        >
          {reviewSubmission.isPending ? "Asking Sage..." : "Get Sage feedback"}
        </Button>
      )}

      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="tutor-feedback">Your feedback (optional)</Label>
          <Textarea
            id="tutor-feedback"
            rows={4}
            value={tutorFeedback}
            onChange={(event) => setTutorFeedback(event.target.value)}
            placeholder="Add or revise feedback before releasing to the student..."
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="grade">Grade (optional)</Label>
          <Input
            id="grade"
            value={grade}
            onChange={(event) => setGrade(event.target.value)}
            className="max-w-32"
          />
        </div>
        <Button
          type="button"
          disabled={releaseFeedback.isPending}
          onClick={() =>
            releaseFeedback.mutate({
              tutorFeedback: tutorFeedback.trim() || undefined,
              grade: grade.trim() || undefined,
            })
          }
        >
          {releaseFeedback.isPending
            ? "Releasing..."
            : isReleased
              ? "Update and re-release"
              : "Release to student"}
        </Button>
      </div>
    </div>
  );
}
