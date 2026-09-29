"use client";

import { use, useState } from "react";
import { IconArrowBackUp, IconGitCompare } from "@tabler/icons-react";

import { Breadcrumbs } from "@/components/layout/Breadcrumbs";
import { CodeReviewView } from "@/components/assignment/CodeReviewView";
import { SubmissionForm } from "@/components/assignment/SubmissionForm";
import { RubricView } from "@/components/rubric/RubricView";
import { SageFeedbackBlock } from "@/components/sage/SageFeedbackBlock";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyAssignment } from "@/hooks/useAssignments";
import { useMyLineComments, useMySubmissions } from "@/hooks/useSubmissions";
import type { FeedbackStatus, Submission } from "@/types/submission";
import { formatDueDate } from "@/lib/dates";
import { cn } from "@/lib/utils";

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
  const submissions = useMySubmissions(id);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [resubmitting, setResubmitting] = useState(false);

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

  // Newest first from the API.
  const versions = submissions.data ?? [];
  const latest = versions[0];
  const selected = versions.find((v) => v.id === selectedId) ?? latest;
  const canResubmit = latest?.releasedAt != null && assignment.data.allowResubmission;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Breadcrumbs
          items={[
            { label: "Courses", href: "/student/courses" },
            { label: assignment.data.courseName, href: `/student/courses/${assignment.data.courseId}?tab=classwork` },
            { label: assignment.data.title },
          ]}
        />
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">{assignment.data.title}</h1>
        <p className="text-sm text-muted-foreground">
          {assignment.data.courseName} · Due {formatDueDate(assignment.data.dueDate)}
          {assignment.data.allowResubmission ? " · Resubmission allowed after feedback" : null}
        </p>
        {assignment.data.description ? (
          <p className="mt-2 text-sm text-muted-foreground">{assignment.data.description}</p>
        ) : null}
      </div>

      <RubricView assignmentId={id} />

      {submissions.isLoading ? (
        <Skeleton className="h-32 w-full" />
      ) : !latest ? (
        <SubmissionForm
          assignmentId={id}
          instructions={{ title: assignment.data.title, description: assignment.data.description }}
        />
      ) : resubmitting ? (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium text-foreground">
            Version {latest.attemptNumber + 1}{" "}
            <span className="font-normal text-muted-foreground">· starts from your last version</span>
          </p>
          <SubmissionForm
            assignmentId={id}
            instructions={{ title: assignment.data.title, description: assignment.data.description }}
            initialContent={latest.content}
            submitLabel={`Submit version ${latest.attemptNumber + 1}`}
            onSubmitted={() => {
              setResubmitting(false);
              setSelectedId(null);
            }}
            onCancel={() => setResubmitting(false)}
          />
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            {versions.length > 1 ? (
              <nav aria-label="Versions" className="flex gap-1 font-mono text-xs">
                {[...versions].reverse().map((version) => (
                  <button
                    key={version.id}
                    type="button"
                    onClick={() => setSelectedId(version.id)}
                    className={cn(
                      "rounded-md border px-2 py-1",
                      version.id === selected.id
                        ? "border-primary bg-primary-soft text-primary"
                        : "border-border text-muted-foreground hover:bg-muted",
                    )}
                  >
                    v{version.attemptNumber}
                  </button>
                ))}
              </nav>
            ) : (
              <span className="font-mono text-xs text-muted-foreground">v{selected.attemptNumber}</span>
            )}
            <div className="flex items-center gap-2">
              <span className="text-xs text-muted-foreground">
                Submitted {new Date(selected.submittedAt).toLocaleString()}
              </span>
              <Badge variant={statusVariant[selected.feedbackStatus]}>
                {selected.releasedAt ? "Feedback returned" : "Awaiting review"}
              </Badge>
            </div>
          </div>

          <SubmissionFeedback
            key={selected.id}
            submission={selected}
            previous={versions.find((v) => v.attemptNumber === selected.attemptNumber - 1)}
          />

          {canResubmit && selected.id === latest.id ? (
            <Card>
              <CardContent className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm text-muted-foreground">
                  Your teacher allows resubmission. Fix what the comments point out and submit a new version.
                </p>
                <Button onClick={() => setResubmitting(true)}>
                  <IconArrowBackUp /> Revise and resubmit
                </Button>
              </CardContent>
            </Card>
          ) : null}
        </div>
      )}
    </div>
  );
}

function SubmissionFeedback({ submission, previous }: { submission: Submission; previous?: Submission }) {
  const released = submission.releasedAt != null;
  const comments = useMyLineComments(submission.id, released);
  const [showChanges, setShowChanges] = useState(false);
  const commentCount = comments.data?.length ?? 0;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm font-medium text-foreground">
          Your code
          {released && commentCount > 0 ? (
            <span className="font-normal text-muted-foreground">
              {" "}
              · {commentCount} line {commentCount === 1 ? "comment" : "comments"}
            </span>
          ) : null}
        </p>
        {previous ? (
          <Button size="sm" variant={showChanges ? "secondary" : "ghost"} onClick={() => setShowChanges((v) => !v)}>
            <IconGitCompare />
            {showChanges ? `Changes since v${previous.attemptNumber}` : "Show changes"}
          </Button>
        ) : null}
      </div>
      <CodeReviewView
        code={submission.content}
        comments={released ? (comments.data ?? []) : []}
        compareTo={previous && showChanges ? previous.content : null}
      />

      {released ? (
        <div className="grid gap-4 md:grid-cols-2">
          {submission.tutorFeedback ? (
            <Card className="md:col-span-2">
              <CardContent>
                <p className="mb-1 text-xs font-medium text-muted-foreground">Teacher feedback</p>
                <p className="whitespace-pre-wrap text-sm text-foreground">{submission.tutorFeedback}</p>
              </CardContent>
            </Card>
          ) : null}
          {submission.sageFeedback ? (
            <div className="md:col-span-2">
              <SageFeedbackBlock feedbackJson={submission.sageFeedback} />
            </div>
          ) : null}
          {submission.grade ? (
            <Card>
              <CardContent>
                <p className="text-xs font-medium text-muted-foreground">Grade</p>
                <p className="font-mono text-2xl font-semibold text-foreground">{submission.grade}</p>
              </CardContent>
            </Card>
          ) : null}
          {submission.rubricScores.length > 0 ? (
            <Card>
              <CardContent className="flex flex-col gap-2">
                <p className="text-xs font-medium text-muted-foreground">
                  Rubric score: {submission.rubricTotalAwarded} / {submission.rubricTotalPossible}
                </p>
                {submission.rubricScores.map((score) => (
                  <div key={score.criterionId} className="flex items-center justify-between text-sm">
                    <span className="text-foreground">{score.criterionName}</span>
                    <span className="font-mono text-muted-foreground">
                      {score.pointsAwarded} / {score.pointsPossible}
                    </span>
                  </div>
                ))}
              </CardContent>
            </Card>
          ) : null}
        </div>
      ) : (
        <Card>
          <CardContent>
            <p className="text-sm text-muted-foreground">Your teacher hasn&apos;t released feedback yet.</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
