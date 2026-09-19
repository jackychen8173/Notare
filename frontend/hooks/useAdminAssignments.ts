"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Assignment } from "@/types/assignment";
import type { Submission } from "@/types/submission";

export const adminAssignmentKeys = {
  detail: (id: string) => ["admin", "assignments", id] as const,
  submissions: (id: string) => ["admin", "assignments", id, "submissions"] as const,
};

async function fetchAdminAssignment(id: string): Promise<Assignment> {
  const res = await api.get<ApiEnvelope<Assignment>>(`/api/admin/assignments/${id}`);
  return res.data.data;
}

async function fetchAdminAssignmentSubmissions(id: string): Promise<Submission[]> {
  const res = await api.get<ApiEnvelope<Submission[]>>(`/api/admin/assignments/${id}/submissions`);
  return res.data.data;
}

export function useAdminAssignment(id: string) {
  return useQuery({
    queryKey: adminAssignmentKeys.detail(id),
    queryFn: () => fetchAdminAssignment(id),
    enabled: !!id,
  });
}

export function useAdminAssignmentSubmissions(id: string) {
  return useQuery({
    queryKey: adminAssignmentKeys.submissions(id),
    queryFn: () => fetchAdminAssignmentSubmissions(id),
    enabled: !!id,
  });
}
