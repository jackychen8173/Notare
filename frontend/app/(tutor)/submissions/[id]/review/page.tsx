"use client";

import { use } from "react";

import { Breadcrumbs } from "@/components/layout/Breadcrumbs";
import { useAssignment } from "@/hooks/useAssignments";
import { SubmissionReview } from "@/components/assignment/SubmissionReview";
import { Skeleton } from "@/components/ui/skeleton";
import { useSubmission } from "@/hooks/useSubmissions";

export default function SubmissionReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const submission = useSubmission(id);
  const assignment = useAssignment(submission.data?.assignmentId ?? "");

  if (submission.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!submission.data) {
    return <p className="text-sm text-muted-foreground">Submission not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Breadcrumbs
          items={[
            { label: "Courses", href: "/courses" },
            ...(assignment.data
              ? [
                  { label: assignment.data.courseName, href: `/courses/${assignment.data.courseId}?tab=classwork` },
                  { label: assignment.data.title, href: `/assignments/${assignment.data.id}` },
                ]
              : []),
            { label: submission.data.studentName },
          ]}
        />
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">Review submission</h1>
      </div>
      <SubmissionReview submission={submission.data} />
    </div>
  );
}
