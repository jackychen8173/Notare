"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Rubric } from "@/types/rubric";

export const rubricKeys = {
  forAssignment: (assignmentId: string) => ["assignments", assignmentId, "rubric"] as const,
  forMyAssignment: (assignmentId: string) => ["student", "assignments", assignmentId, "rubric"] as const,
};

async function fetchRubric(assignmentId: string): Promise<Rubric | null> {
  try {
    const res = await api.get<ApiEnvelope<Rubric>>(`/api/assignments/${assignmentId}/rubric`);
    return res.data.data;
  } catch (error: unknown) {
    if (error && typeof error === "object" && "response" in error) {
      const response = (error as { response?: { status?: number } }).response;
      if (response?.status === 404) return null;
    }
    throw error;
  }
}

async function fetchMyRubric(assignmentId: string): Promise<Rubric | null> {
  try {
    const res = await api.get<ApiEnvelope<Rubric>>(`/api/student/assignments/${assignmentId}/rubric`);
    return res.data.data;
  } catch (error: unknown) {
    if (error && typeof error === "object" && "response" in error) {
      const response = (error as { response?: { status?: number } }).response;
      if (response?.status === 404) return null;
    }
    throw error;
  }
}

export function useRubric(assignmentId: string) {
  return useQuery({
    queryKey: rubricKeys.forAssignment(assignmentId),
    queryFn: () => fetchRubric(assignmentId),
    enabled: !!assignmentId,
  });
}

export function useMyRubric(assignmentId: string) {
  return useQuery({
    queryKey: rubricKeys.forMyAssignment(assignmentId),
    queryFn: () => fetchMyRubric(assignmentId),
    enabled: !!assignmentId,
  });
}

export interface CreateRubricInput {
  title: string;
  criteria: { name: string; description?: string; pointsPossible: number }[];
}

export function useCreateRubric(assignmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateRubricInput) => {
      const res = await api.post<ApiEnvelope<Rubric>>(`/api/assignments/${assignmentId}/rubric`, input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: rubricKeys.forAssignment(assignmentId) });
    },
  });
}

export function useDeleteRubric(assignmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await api.delete(`/api/assignments/${assignmentId}/rubric`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: rubricKeys.forAssignment(assignmentId) });
    },
  });
}
