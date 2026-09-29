"use client";

import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Assignment } from "@/types/assignment";

export const assignmentKeys = {
  forCourse: (courseId: string) => ["courses", courseId, "assignments"] as const,
  detail: (id: string) => ["assignments", id] as const,
  forMyCourse: (courseId: string) => ["student", "courses", courseId, "assignments"] as const,
  mineDetail: (id: string) => ["student", "assignments", id] as const,
};

async function fetchCourseAssignments(courseId: string): Promise<Assignment[]> {
  const res = await api.get<ApiEnvelope<Assignment[]>>(`/api/courses/${courseId}/assignments`);
  return res.data.data;
}

async function fetchAssignment(id: string): Promise<Assignment> {
  const res = await api.get<ApiEnvelope<Assignment>>(`/api/assignments/${id}`);
  return res.data.data;
}

async function fetchMyCourseAssignments(courseId: string): Promise<Assignment[]> {
  const res = await api.get<ApiEnvelope<Assignment[]>>(
    `/api/student/courses/${courseId}/assignments`,
  );
  return res.data.data;
}

async function fetchMyAssignment(id: string): Promise<Assignment> {
  const res = await api.get<ApiEnvelope<Assignment>>(`/api/student/assignments/${id}`);
  return res.data.data;
}

export function useCourseAssignments(courseId: string) {
  return useQuery({
    queryKey: assignmentKeys.forCourse(courseId),
    queryFn: () => fetchCourseAssignments(courseId),
    enabled: !!courseId,
  });
}

/**
 * Assignments across several courses at once (dashboards), sharing each course's cache entry with
 * the course page. `mine` switches to the student-facing endpoints.
 */
export function useAssignmentsForCourses(courseIds: string[], mine = false) {
  return useQueries({
    queries: courseIds.map((courseId) => ({
      queryKey: mine ? assignmentKeys.forMyCourse(courseId) : assignmentKeys.forCourse(courseId),
      queryFn: () => (mine ? fetchMyCourseAssignments(courseId) : fetchCourseAssignments(courseId)),
    })),
    combine: (results) => ({
      data: results.flatMap((result) => result.data ?? []),
      isLoading: results.some((result) => result.isLoading),
    }),
  });
}

export function useAssignment(id: string) {
  return useQuery({
    queryKey: assignmentKeys.detail(id),
    queryFn: () => fetchAssignment(id),
    enabled: !!id,
  });
}

export function useMyCourseAssignments(courseId: string) {
  return useQuery({
    queryKey: assignmentKeys.forMyCourse(courseId),
    queryFn: () => fetchMyCourseAssignments(courseId),
    enabled: !!courseId,
  });
}

export function useMyAssignment(id: string) {
  return useQuery({
    queryKey: assignmentKeys.mineDetail(id),
    queryFn: () => fetchMyAssignment(id),
    enabled: !!id,
  });
}

export interface CreateAssignmentInput {
  title: string;
  description?: string;
  dueDate: string;
  topicId?: string;
  gradeCategoryId?: string;
  allowResubmission?: boolean;
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

export interface AssignmentSettingsInput {
  allowResubmission: boolean;
  /** null ungroups the assignment. */
  topicId: string | null;
}

export function useUpdateAssignmentSettings(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: AssignmentSettingsInput) => {
      const res = await api.put<ApiEnvelope<Assignment>>(`/api/assignments/${id}/settings`, input);
      return res.data.data;
    },
    onSuccess: (assignment) => {
      queryClient.setQueryData(assignmentKeys.detail(id), assignment);
      queryClient.invalidateQueries({ queryKey: assignmentKeys.forCourse(assignment.courseId) });
    },
  });
}
