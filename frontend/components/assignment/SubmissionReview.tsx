"use client";

import { useState } from "react";

import { SageFeedbackBlock } from "@/components/sage/SageFeedbackBlock";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useReleaseFeedback, useReviewSubmission } from "@/hooks/useSubmissions";
import type { FeedbackStatus, Submission } from "@/types/submission";

const statusVariant: Record<FeedbackStatus, "default" | "secondary"> = {
  PENDING: "secondary",
  APPROVED: "default",
  REVISED: "default",
};

interface SubmissionReviewProps {
  submission: Submission;
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
