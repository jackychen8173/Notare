"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { ProgressSummary } from "@/types/sage";

async function fetchProgressSummary(studentId: string): Promise<ProgressSummary> {
  const res = await api.get<ApiEnvelope<ProgressSummary>>(
    `/api/sage/student-progress/${studentId}`,
  );
  return res.data.data;
}

interface SageProgressSummaryProps {
  studentId: string;
}

export function SageProgressSummary({ studentId }: SageProgressSummaryProps) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["sage", "student-progress", studentId],
    queryFn: () => fetchProgressSummary(studentId),
    enabled: !!studentId,
    retry: false,
  });

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Asking Sage for a summary...</p>;
  }

  if (isError || !data) {
    return (
      <p className="text-sm text-muted-foreground">
        Not enough session or submission history yet for a progress summary.
      </p>
    );
  }

  return (
    <div className="rounded-card border-hairline border-sage-border bg-sage-surface p-4">
      <p className="mb-1 text-xs font-medium text-sage-text">Sage progress summary</p>
      <p className="text-sm text-sage-text">{data.summary}</p>
    </div>
  );
}
