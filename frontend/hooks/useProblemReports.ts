"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { adminDashboardKeys } from "@/hooks/useAdminDashboard";
import { api, type ApiEnvelope } from "@/lib/api";
import type { ProblemReport, ProblemReportCategory, ProblemReportStatus } from "@/types/problemReport";

export const problemReportKeys = {
  all: ["admin", "reports"] as const,
  list: (status: ProblemReportStatus | null) => ["admin", "reports", status] as const,
};

export interface CreateProblemReportInput {
  category: ProblemReportCategory;
  message: string;
  pageUrl?: string;
}

export function useCreateProblemReport() {
  return useMutation({
    mutationFn: async (input: CreateProblemReportInput) => {
      const res = await api.post<ApiEnvelope<ProblemReport>>("/api/reports", input);
      return res.data.data;
    },
  });
}

export function useAdminProblemReports(status: ProblemReportStatus | null) {
  return useQuery({
    queryKey: problemReportKeys.list(status),
    queryFn: async () => {
      const res = await api.get<ApiEnvelope<ProblemReport[]>>("/api/admin/reports", {
        params: status ? { status } : {},
      });
      return res.data.data;
    },
  });
}

export function useSetProblemReportStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, resolved }: { id: string; resolved: boolean }) => {
      const res = await api.post<ApiEnvelope<ProblemReport>>(
        `/api/admin/reports/${id}/${resolved ? "resolve" : "reopen"}`,
      );
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: problemReportKeys.all });
      queryClient.invalidateQueries({ queryKey: adminDashboardKeys.stats });
    },
  });
}
