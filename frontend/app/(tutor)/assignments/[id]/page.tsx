"use client";

import { use } from "react";
import Link from "next/link";

import { IconInbox } from "@tabler/icons-react";

import { EmptyState } from "@/components/layout/EmptyState";
import { Breadcrumbs } from "@/components/layout/Breadcrumbs";
import { AssignmentFormDialog } from "@/components/assignment/AssignmentFormDialog";
import { AssignmentSettingsCard } from "@/components/assignment/AssignmentSettingsCard";
import { DataFilesPanel } from "@/components/assignment/DataFilesPanel";
import { AssignmentTiming, LateBadge } from "@/components/assignment/AssignmentTiming";
import { Button } from "@/components/ui/button";
import { RubricPanel } from "@/components/rubric/RubricPanel";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useAssignment } from "@/hooks/useAssignments";
import { useAssignmentSubmissions } from "@/hooks/useSubmissions";
import type { FeedbackStatus, Submission } from "@/types/submission";

/** "18 / 20", "14.4 / 20 after −20%", or the old free-text grade. */
function scoreLabel(submission: Submission): string {
  if (submission.pointsEarned != null && submission.pointsPossible != null) {
    const after = submission.pointsAfterPenalty ?? submission.pointsEarned;
    return submission.latePenaltyPercent > 0
      ? `${after} / ${submission.pointsPossible} (${submission.pointsEarned} before −${submission.latePenaltyPercent}%)`
      : `${after} / ${submission.pointsPossible}`;
  }
  return submission.grade ?? "—";
}

const statusVariant: Record<FeedbackStatus, "default" | "secondary"> = {
  PENDING: "secondary",
  APPROVED: "default",
  REVISED: "default",
};

export default function AssignmentDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const assignment = useAssignment(id);
  const submissions = useAssignmentSubmissions(id);

  if (assignment.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (!assignment.data) {
    return <p className="text-sm text-muted-foreground">Assignment not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Breadcrumbs
          items={[
            { label: "Courses", href: "/courses" },
            { label: assignment.data.courseName, href: `/courses/${assignment.data.courseId}?tab=classwork` },
            { label: assignment.data.title },
          ]}
        />
        <div className="flex flex-wrap items-start justify-between gap-3">
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">{assignment.data.title}</h1>
          <AssignmentFormDialog
            courseId={assignment.data.courseId}
            timeZone={assignment.data.timeZone}
            assignment={assignment.data}
            trigger={<Button variant="outline">Edit</Button>}
          />
        </div>
        <div className="mt-1">
          <AssignmentTiming assignment={assignment.data} audience="tutor" />
        </div>
        {assignment.data.description ? (
          <p className="mt-2 text-sm text-muted-foreground">{assignment.data.description}</p>
        ) : null}
      </div>

      <AssignmentSettingsCard assignment={assignment.data} />

      <DataFilesPanel assignmentId={assignment.data.id} />

      <div>
        <h2 className="mb-3 text-lg font-semibold text-foreground">Submissions</h2>
        {submissions.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : submissions.data && submissions.data.length > 0 ? (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Student</TableHead>
                <TableHead>Version</TableHead>
                <TableHead>Submitted</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Score</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {[...submissions.data]
                .sort(
                  (a, b) =>
                    a.studentName.localeCompare(b.studentName) || b.attemptNumber - a.attemptNumber,
                )
                .map((submission) => (
                <TableRow key={submission.id}>
                  <TableCell>
                    <Link href={`/submissions/${submission.id}/review`} className="font-medium text-foreground">
                      {submission.studentName}
                    </Link>
                  </TableCell>
                  <TableCell className="font-mono text-xs text-muted-foreground">v{submission.attemptNumber}</TableCell>
                  <TableCell className="text-muted-foreground">
                    <span className="flex flex-wrap items-center gap-2">
                      {new Date(submission.submittedAt).toLocaleString()}
                      {submission.late ? (
                        <LateBadge daysLate={submission.daysLate} latePenaltyPercent={submission.latePenaltyPercent} />
                      ) : null}
                    </span>
                  </TableCell>
                  <TableCell>
                    <Badge variant={statusVariant[submission.feedbackStatus]}>
                      {submission.releasedAt ? submission.feedbackStatus : "To review"}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-mono text-xs text-muted-foreground">{scoreLabel(submission)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        ) : (
          <EmptyState icon={IconInbox} title="No submissions yet" description="Student work shows up here once it's turned in." />
        )}
      </div>

      <RubricPanel assignmentId={id} />
    </div>
  );
}
