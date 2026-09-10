"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Announcement } from "@/types/announcement";

export const announcementKeys = {
  forCourse: (courseId: string) => ["courses", courseId, "announcements"] as const,
  forMyCourse: (courseId: string) => ["student", "courses", courseId, "announcements"] as const,
};

async function fetchAnnouncements(courseId: string): Promise<Announcement[]> {
  const res = await api.get<ApiEnvelope<Announcement[]>>(`/api/courses/${courseId}/announcements`);
  return res.data.data;
}

async function fetchMyCourseAnnouncements(courseId: string): Promise<Announcement[]> {
  const res = await api.get<ApiEnvelope<Announcement[]>>(`/api/student/courses/${courseId}/announcements`);
  return res.data.data;
}

export function useAnnouncements(courseId: string) {
  return useQuery({
    queryKey: announcementKeys.forCourse(courseId),
    queryFn: () => fetchAnnouncements(courseId),
    enabled: !!courseId,
  });
}

export function useMyCourseAnnouncements(courseId: string) {
  return useQuery({
    queryKey: announcementKeys.forMyCourse(courseId),
    queryFn: () => fetchMyCourseAnnouncements(courseId),
    enabled: !!courseId,
  });
}

export function useCreateAnnouncement(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (content: string) => {
      const res = await api.post<ApiEnvelope<Announcement>>(`/api/courses/${courseId}/announcements`, { content });
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: announcementKeys.forCourse(courseId) });
    },
  });
}

export function useDeleteAnnouncement(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/announcements/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: announcementKeys.forCourse(courseId) });
    },
  });
}
