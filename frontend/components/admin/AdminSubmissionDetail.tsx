import { SageFeedbackBlock } from "@/components/sage/SageFeedbackBlock";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { FeedbackStatus, Submission } from "@/types/submission";

const statusVariant: Record<FeedbackStatus, "default" | "secondary"> = {
  PENDING: "secondary",
  APPROVED: "default",
  REVISED: "default",
};

interface AdminSubmissionDetailProps {
  submission: Submission;
}

export function AdminSubmissionDetail({ submission }: AdminSubmissionDetailProps) {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-muted-foreground">Submitted by</p>
          <p className="font-medium text-foreground">{submission.studentName}</p>
        </div>
        <Badge variant={statusVariant[submission.feedbackStatus]}>{submission.feedbackStatus}</Badge>
      </div>

      <div className="flex flex-col gap-1.5">
        <p className="text-sm font-medium text-foreground">Submitted code</p>
        <pre className="overflow-x-auto rounded-card border-hairline border-border bg-muted/40 p-4 font-mono text-xs text-foreground">
          {submission.content}
        </pre>
      </div>

      {submission.rubricScores.length > 0 ? (
        <Card>
          <CardContent className="flex flex-col gap-2">
            <p className="text-sm font-medium text-foreground">Rubric scores</p>
            {submission.rubricScores.map((score) => (
              <div key={score.criterionId} className="flex items-center justify-between">
                <p className="text-sm text-foreground">{score.criterionName}</p>
                <p className="text-sm text-muted-foreground">
                  {score.pointsAwarded} / {score.pointsPossible}
                </p>
              </div>
            ))}
          </CardContent>
        </Card>
      ) : null}

      {submission.sageFeedback ? <SageFeedbackBlock feedbackJson={submission.sageFeedback} /> : null}

      {submission.tutorFeedback ? (
        <div className="flex flex-col gap-1.5">
          <p className="text-sm font-medium text-foreground">Tutor feedback</p>
          <p className="text-sm text-muted-foreground">{submission.tutorFeedback}</p>
        </div>
      ) : null}

      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <span>Grade: {submission.grade ?? "—"}</span>
        <span>·</span>
        <span>
          {submission.releasedAt
            ? `Released ${new Date(submission.releasedAt).toLocaleString()}`
            : "Not yet released to student"}
        </span>
      </div>
    </div>
  );
}
