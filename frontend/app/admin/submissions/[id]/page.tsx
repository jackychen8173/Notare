"use client";

import { use } from "react";

import { AdminSubmissionDetail } from "@/components/admin/AdminSubmissionDetail";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminSubmission } from "@/hooks/useAdminSubmissions";

export default function AdminSubmissionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const submission = useAdminSubmission(id);

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
      <h1 className="text-2xl font-medium text-foreground">Submission</h1>
      <AdminSubmissionDetail submission={submission.data} />
    </div>
  );
}
