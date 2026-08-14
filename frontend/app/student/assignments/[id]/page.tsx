"use client";

import { use } from "react";

import { SubmissionForm } from "@/components/assignment/SubmissionForm";
import { SageFeedbackBlock } from "@/components/sage/SageFeedbackBlock";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyAssignment } from "@/hooks/useAssignments";
import { useMySubmission } from "@/hooks/useSubmissions";
import type { FeedbackStatus } from "@/types/submission";

const statusVariant: Record<FeedbackStatus, "default" | "secondary"> = {
  PENDING: "secondary",
  APPROVED: "default",
  REVISED: "default",
};

export default function StudentAssignmentDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const assignment = useMyAssignment(id);
  const submission = useMySubmission(id);

  if (assignment.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  if (!assignment.data) {
    return <p className="text-sm text-muted-foreground">Assignment not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-medium text-foreground">{assignment.data.title}</h1>
        <p className="text-sm text-muted-foreground">
          {assignment.data.courseName} · Due{" "}
          {new Date(assignment.data.dueDate).toLocaleDateString()}
        </p>
        {assignment.data.description ? (
          <p className="mt-2 text-sm text-muted-foreground">{assignment.data.description}</p>
        ) : null}
      </div>

      {submission.isLoading ? (
        <Skeleton className="h-32 w-full" />
      ) : submission.data ? (
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Submitted {new Date(submission.data.submittedAt).toLocaleString()}
            </p>
            <Badge variant={statusVariant[submission.data.feedbackStatus]}>
              {submission.data.releasedAt ? submission.data.feedbackStatus : "Awaiting review"}
            </Badge>
          </div>

          <div className="flex flex-col gap-1.5">
            <p className="text-sm font-medium text-foreground">Your submission</p>
            <pre className="overflow-x-auto rounded-card border-hairline border-border bg-muted/40 p-4 font-mono text-xs text-foreground">
              {submission.data.content}
            </pre>
          </div>

          {submission.data.releasedAt ? (
            <>
              {submission.data.sageFeedback ? (
                <SageFeedbackBlock feedbackJson={submission.data.sageFeedback} />
              ) : null}
              {submission.data.tutorFeedback ? (
                <Card>
                  <CardContent>
                    <p className="mb-1 text-xs font-medium text-muted-foreground">Tutor feedback</p>
                    <p className="text-sm text-foreground">{submission.data.tutorFeedback}</p>
                  </CardContent>
                </Card>
              ) : null}
              {submission.data.grade ? (
                <p className="text-sm text-foreground">
                  <span className="font-medium">Grade:</span> {submission.data.grade}
                </p>
              ) : null}
            </>
          ) : (
            <Card>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  Your tutor hasn&apos;t released feedback yet.
                </p>
              </CardContent>
            </Card>
          )}
        </div>
      ) : (
        <SubmissionForm assignmentId={id} />
      )}
    </div>
  );
}
