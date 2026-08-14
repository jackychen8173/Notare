"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { PendingReviews } from "@/types/sage";
import type { Submission } from "@/types/submission";

export const submissionKeys = {
  forAssignment: (assignmentId: string) => ["assignments", assignmentId, "submissions"] as const,
  detail: (id: string) => ["submissions", id] as const,
  pendingReviews: ["sage", "pending-reviews"] as const,
  mineForAssignment: (assignmentId: string) =>
    ["student", "assignments", assignmentId, "submission"] as const,
};

async function fetchAssignmentSubmissions(assignmentId: string): Promise<Submission[]> {
  const res = await api.get<ApiEnvelope<Submission[]>>(
    `/api/assignments/${assignmentId}/submissions`,
  );
  return res.data.data;
}

async function fetchSubmission(id: string): Promise<Submission> {
  const res = await api.get<ApiEnvelope<Submission>>(`/api/submissions/${id}`);
  return res.data.data;
}

async function fetchPendingReviews(): Promise<PendingReviews> {
  const res = await api.get<ApiEnvelope<PendingReviews>>("/api/sage/pending-reviews");
  return res.data.data;
}

export function useAssignmentSubmissions(assignmentId: string) {
  return useQuery({
    queryKey: submissionKeys.forAssignment(assignmentId),
    queryFn: () => fetchAssignmentSubmissions(assignmentId),
    enabled: !!assignmentId,
  });
}

export function useSubmission(id: string) {
  return useQuery({
    queryKey: submissionKeys.detail(id),
    queryFn: () => fetchSubmission(id),
    enabled: !!id,
  });
}

export function usePendingReviews() {
  return useQuery({ queryKey: submissionKeys.pendingReviews, queryFn: fetchPendingReviews });
}

async function fetchMySubmission(assignmentId: string): Promise<Submission | null> {
  const res = await api.get<ApiEnvelope<Submission | null>>(
    `/api/student/assignments/${assignmentId}/submission`,
  );
  return res.data.data;
}

export function useMySubmission(assignmentId: string) {
  return useQuery({
    queryKey: submissionKeys.mineForAssignment(assignmentId),
    queryFn: () => fetchMySubmission(assignmentId),
    enabled: !!assignmentId,
  });
}

export function useSubmitAssignment(assignmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (content: string) => {
      const res = await api.post<ApiEnvelope<Submission>>(
        `/api/assignments/${assignmentId}/submit`,
        { content },
      );
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: submissionKeys.mineForAssignment(assignmentId) });
    },
  });
}

export function useReviewSubmission(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiEnvelope<Submission>>("/api/sage/review-submission", {
        submissionId: id,
      });
      return res.data.data;
    },
    onSuccess: (submission) => {
      queryClient.setQueryData(submissionKeys.detail(id), submission);
    },
  });
}

export interface ReleaseFeedbackInput {
  tutorFeedback?: string;
  grade?: string;
}

export function useReleaseFeedback(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: ReleaseFeedbackInput) => {
      const res = await api.patch<ApiEnvelope<Submission>>(`/api/submissions/${id}/release`, input);
      return res.data.data;
    },
    onSuccess: (submission) => {
      queryClient.setQueryData(submissionKeys.detail(id), submission);
      queryClient.invalidateQueries({ queryKey: submissionKeys.pendingReviews });
    },
  });
}
