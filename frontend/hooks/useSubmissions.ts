"use client";

import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { PendingReviews } from "@/types/sage";
import type { LineComment, Submission } from "@/types/submission";

export const submissionKeys = {
  forAssignment: (assignmentId: string) => ["assignments", assignmentId, "submissions"] as const,
  detail: (id: string) => ["submissions", id] as const,
  pendingReviews: ["sage", "pending-reviews"] as const,
  mineForAssignment: (assignmentId: string) =>
    ["student", "assignments", assignmentId, "submission"] as const,
  mineAllForAssignment: (assignmentId: string) =>
    ["student", "assignments", assignmentId, "submissions"] as const,
  lineComments: (submissionId: string) => ["submissions", submissionId, "line-comments"] as const,
  myLineComments: (submissionId: string) =>
    ["student", "submissions", submissionId, "line-comments"] as const,
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

/** The student's own submission (or null) for each assignment, keyed by assignment ID. */
export function useMySubmissionsFor(assignmentIds: string[]) {
  return useQueries({
    queries: assignmentIds.map((assignmentId) => ({
      queryKey: submissionKeys.mineForAssignment(assignmentId),
      queryFn: () => fetchMySubmission(assignmentId),
    })),
    combine: (results) => {
      const byAssignment: Record<string, Submission | null | undefined> = {};
      results.forEach((result, index) => {
        byAssignment[assignmentIds[index]] = result.data;
      });
      return byAssignment;
    },
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
      queryClient.invalidateQueries({ queryKey: submissionKeys.mineAllForAssignment(assignmentId) });
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
      // Sage's review also drafts line comments.
      queryClient.invalidateQueries({ queryKey: submissionKeys.lineComments(id) });
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

export interface RubricScoreInput {
  criterionId: string;
  pointsAwarded: number;
}

export function useUpdateRubricScores(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (scores: RubricScoreInput[]) => {
      const res = await api.put<ApiEnvelope<Submission>>(`/api/submissions/${id}/rubric-scores`, { scores });
      return res.data.data;
    },
    onSuccess: (submission) => {
      queryClient.setQueryData(submissionKeys.detail(id), submission);
    },
  });
}

/** Every version the student submitted for an assignment, newest first. */
export function useMySubmissions(assignmentId: string) {
  return useQuery({
    queryKey: submissionKeys.mineAllForAssignment(assignmentId),
    queryFn: async () => {
      const res = await api.get<ApiEnvelope<Submission[]>>(
        `/api/student/assignments/${assignmentId}/submissions`,
      );
      return res.data.data;
    },
    enabled: !!assignmentId,
  });
}

export function useMyLineComments(submissionId: string, enabled = true) {
  return useQuery({
    queryKey: submissionKeys.myLineComments(submissionId),
    queryFn: async () => {
      const res = await api.get<ApiEnvelope<LineComment[]>>(
        `/api/student/submissions/${submissionId}/line-comments`,
      );
      return res.data.data;
    },
    enabled: !!submissionId && enabled,
  });
}

export function useLineComments(submissionId: string) {
  return useQuery({
    queryKey: submissionKeys.lineComments(submissionId),
    queryFn: async () => {
      const res = await api.get<ApiEnvelope<LineComment[]>>(`/api/submissions/${submissionId}/line-comments`);
      return res.data.data;
    },
    enabled: !!submissionId,
  });
}

/** Add, edit (which also accepts a Sage suggestion) and delete line comments on one submission. */
export function useLineCommentMutations(submissionId: string) {
  const queryClient = useQueryClient();
  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: submissionKeys.lineComments(submissionId) });

  const add = useMutation({
    mutationFn: async (input: { lineNumber: number; body: string }) =>
      (await api.post<ApiEnvelope<LineComment>>(`/api/submissions/${submissionId}/line-comments`, input)).data
        .data,
    onSuccess: invalidate,
  });
  const update = useMutation({
    mutationFn: async ({ id, body }: { id: string; body: string }) =>
      (await api.patch<ApiEnvelope<LineComment>>(`/api/line-comments/${id}`, { body })).data.data,
    onSuccess: invalidate,
  });
  const remove = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/line-comments/${id}`);
    },
    onSuccess: invalidate,
  });

  return { add, update, remove };
}
