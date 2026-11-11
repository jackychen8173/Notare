"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Gradebook, StudentGrades } from "@/types/gradebook";

export const gradebookKeys = {
  course: (courseId: string) => ["courses", courseId, "gradebook"] as const,
  mine: (courseId: string) => ["student", "courses", courseId, "grades"] as const,
};

/** Every student's grades in a course (tutor). */
export function useGradebook(courseId: string) {
  return useQuery({
    queryKey: gradebookKeys.course(courseId),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Gradebook>>(`/api/courses/${courseId}/gradebook`)).data.data,
    enabled: !!courseId,
  });
}

/** The signed-in student's own grades in a course. */
export function useMyGrades(courseId: string) {
  return useQuery({
    queryKey: gradebookKeys.mine(courseId),
    queryFn: async () =>
      (await api.get<ApiEnvelope<StudentGrades>>(`/api/student/courses/${courseId}/grades`)).data.data,
    enabled: !!courseId,
  });
}
