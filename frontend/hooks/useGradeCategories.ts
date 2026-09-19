"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { GradeCategory } from "@/types/gradeCategory";

export const gradeCategoryKeys = {
  forCourse: (courseId: string) => ["courses", courseId, "grade-categories"] as const,
};

async function fetchGradeCategories(courseId: string): Promise<GradeCategory[]> {
  const res = await api.get<ApiEnvelope<GradeCategory[]>>(`/api/courses/${courseId}/grade-categories`);
  return res.data.data;
}

export function useGradeCategories(courseId: string) {
  return useQuery({
    queryKey: gradeCategoryKeys.forCourse(courseId),
    queryFn: () => fetchGradeCategories(courseId),
    enabled: !!courseId,
  });
}

export interface GradeCategoryInput {
  name: string;
  weightPercent: number;
}

export function useCreateGradeCategory(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: GradeCategoryInput) => {
      const res = await api.post<ApiEnvelope<GradeCategory>>(`/api/courses/${courseId}/grade-categories`, input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: gradeCategoryKeys.forCourse(courseId) });
    },
  });
}

export function useDeleteGradeCategory(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/grade-categories/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: gradeCategoryKeys.forCourse(courseId) });
    },
  });
}
