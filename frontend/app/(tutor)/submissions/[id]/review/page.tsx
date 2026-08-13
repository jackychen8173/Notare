"use client";

import { use } from "react";

import { SubmissionReview } from "@/components/assignment/SubmissionReview";
import { Skeleton } from "@/components/ui/skeleton";
import { useSubmission } from "@/hooks/useSubmissions";

export default function SubmissionReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const submission = useSubmission(id);

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
      <h1 className="text-2xl font-medium text-foreground">Review submission</h1>
      <SubmissionReview submission={submission.data} />
    </div>
  );
}
