"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Assignment } from "@/types/assignment";

export const assignmentKeys = {
  forCourse: (courseId: string) => ["courses", courseId, "assignments"] as const,
  detail: (id: string) => ["assignments", id] as const,
};

async function fetchCourseAssignments(courseId: string): Promise<Assignment[]> {
  const res = await api.get<ApiEnvelope<Assignment[]>>(`/api/courses/${courseId}/assignments`);
  return res.data.data;
}

async function fetchAssignment(id: string): Promise<Assignment> {
  const res = await api.get<ApiEnvelope<Assignment>>(`/api/assignments/${id}`);
  return res.data.data;
}

export function useCourseAssignments(courseId: string) {
  return useQuery({
    queryKey: assignmentKeys.forCourse(courseId),
    queryFn: () => fetchCourseAssignments(courseId),
    enabled: !!courseId,
  });
}

export function useAssignment(id: string) {
  return useQuery({
    queryKey: assignmentKeys.detail(id),
    queryFn: () => fetchAssignment(id),
    enabled: !!id,
  });
}

export interface CreateAssignmentInput {
  title: string;
  description?: string;
  dueDate: string;
}

export function useCreateAssignment(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateAssignmentInput) => {
      const res = await api.post<ApiEnvelope<Assignment>>(
        `/api/courses/${courseId}/assignments`,
        input,
      );
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assignmentKeys.forCourse(courseId) });
    },
  });
}
