"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Topic } from "@/types/topic";

export const topicKeys = {
  forCourse: (courseId: string) => ["courses", courseId, "topics"] as const,
};

async function fetchTopics(courseId: string): Promise<Topic[]> {
  const res = await api.get<ApiEnvelope<Topic[]>>(`/api/courses/${courseId}/topics`);
  return res.data.data;
}

export function useTopics(courseId: string) {
  return useQuery({
    queryKey: topicKeys.forCourse(courseId),
    queryFn: () => fetchTopics(courseId),
    enabled: !!courseId,
  });
}

export function useCreateTopic(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (name: string) => {
      const res = await api.post<ApiEnvelope<Topic>>(`/api/courses/${courseId}/topics`, { name });
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: topicKeys.forCourse(courseId) });
    },
  });
}

export function useRenameTopic(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, name }: { id: string; name: string }) => {
      const res = await api.patch<ApiEnvelope<Topic>>(`/api/topics/${id}`, { name });
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: topicKeys.forCourse(courseId) });
    },
  });
}

export function useDeleteTopic(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/topics/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: topicKeys.forCourse(courseId) });
    },
  });
}
