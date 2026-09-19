"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Submission } from "@/types/submission";

export const adminSubmissionKeys = {
  detail: (id: string) => ["admin", "submissions", id] as const,
};

async function fetchAdminSubmission(id: string): Promise<Submission> {
  const res = await api.get<ApiEnvelope<Submission>>(`/api/admin/submissions/${id}`);
  return res.data.data;
}

export function useAdminSubmission(id: string) {
  return useQuery({
    queryKey: adminSubmissionKeys.detail(id),
    queryFn: () => fetchAdminSubmission(id),
    enabled: !!id,
  });
}
