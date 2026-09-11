"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Material } from "@/types/material";

export const materialKeys = {
  forCourse: (courseId: string) => ["courses", courseId, "materials"] as const,
  forMyCourse: (courseId: string) => ["student", "courses", courseId, "materials"] as const,
};

async function fetchMaterials(courseId: string): Promise<Material[]> {
  const res = await api.get<ApiEnvelope<Material[]>>(`/api/courses/${courseId}/materials`);
  return res.data.data;
}

async function fetchMyCourseMaterials(courseId: string): Promise<Material[]> {
  const res = await api.get<ApiEnvelope<Material[]>>(`/api/student/courses/${courseId}/materials`);
  return res.data.data;
}

export function useMaterials(courseId: string) {
  return useQuery({
    queryKey: materialKeys.forCourse(courseId),
    queryFn: () => fetchMaterials(courseId),
    enabled: !!courseId,
  });
}

export function useMyCourseMaterials(courseId: string) {
  return useQuery({
    queryKey: materialKeys.forMyCourse(courseId),
    queryFn: () => fetchMyCourseMaterials(courseId),
    enabled: !!courseId,
  });
}

export interface CreateMaterialInput {
  title: string;
  description?: string;
  url?: string;
  topicId?: string;
}

export function useCreateMaterial(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateMaterialInput) => {
      const res = await api.post<ApiEnvelope<Material>>(`/api/courses/${courseId}/materials`, input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: materialKeys.forCourse(courseId) });
    },
  });
}

export function useDeleteMaterial(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/materials/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: materialKeys.forCourse(courseId) });
    },
  });
}
