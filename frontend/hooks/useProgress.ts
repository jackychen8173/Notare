"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { CourseProgress, StudentProgress } from "@/types/progress";

export const progressKeys = {
  course: (courseId: string) => ["courses", courseId, "progress"] as const,
  mine: (courseId: string) => ["student", "courses", courseId, "progress"] as const,
};

/** The whole class, per unit (tutor). */
export function useCourseProgress(courseId: string) {
  return useQuery({
    queryKey: progressKeys.course(courseId),
    queryFn: async () =>
      (await api.get<ApiEnvelope<CourseProgress>>(`/api/courses/${courseId}/progress`)).data.data,
    enabled: !!courseId,
  });
}

/** The signed-in student's own progress in a course, per unit. */
export function useMyProgress(courseId: string) {
  return useQuery({
    queryKey: progressKeys.mine(courseId),
    queryFn: async () =>
      (await api.get<ApiEnvelope<StudentProgress>>(`/api/student/courses/${courseId}/progress`)).data.data,
    enabled: !!courseId,
  });
}
