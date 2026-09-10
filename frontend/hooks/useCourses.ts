"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Course } from "@/types/course";
import type { Student } from "@/types/user";

export const courseKeys = {
  all: ["courses"] as const,
  list: (archived: boolean) => ["courses", "list", archived] as const,
  detail: (id: string) => ["courses", id] as const,
  students: (id: string) => ["courses", id, "students"] as const,
  mine: ["student", "courses"] as const,
  mineDetail: (id: string) => ["student", "courses", id] as const,
};

async function fetchCourses(archived: boolean): Promise<Course[]> {
  const res = await api.get<ApiEnvelope<Course[]>>("/api/courses", { params: { archived } });
  return res.data.data;
}

async function fetchCourse(id: string): Promise<Course> {
  const res = await api.get<ApiEnvelope<Course>>(`/api/courses/${id}`);
  return res.data.data;
}

async function fetchEnrolledStudents(id: string): Promise<Student[]> {
  const res = await api.get<ApiEnvelope<Student[]>>(`/api/courses/${id}/students`);
  return res.data.data;
}

async function fetchMyCourses(): Promise<Course[]> {
  const res = await api.get<ApiEnvelope<Course[]>>("/api/student/courses");
  return res.data.data;
}

async function fetchMyCourse(id: string): Promise<Course> {
  const res = await api.get<ApiEnvelope<Course>>(`/api/student/courses/${id}`);
  return res.data.data;
}

export function useCourses(archived = false) {
  return useQuery({ queryKey: courseKeys.list(archived), queryFn: () => fetchCourses(archived) });
}

export function useCourse(id: string) {
  return useQuery({ queryKey: courseKeys.detail(id), queryFn: () => fetchCourse(id), enabled: !!id });
}

export function useMyCourses() {
  return useQuery({ queryKey: courseKeys.mine, queryFn: fetchMyCourses });
}

export function useMyCourse(id: string) {
  return useQuery({
    queryKey: courseKeys.mineDetail(id),
    queryFn: () => fetchMyCourse(id),
    enabled: !!id,
  });
}

export function useEnrolledStudents(id: string) {
  return useQuery({
    queryKey: courseKeys.students(id),
    queryFn: () => fetchEnrolledStudents(id),
    enabled: !!id,
  });
}

export interface CreateCourseInput {
  name: string;
  subject: string;
  description?: string;
}

export function useCreateCourse() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateCourseInput) => {
      const res = await api.post<ApiEnvelope<Course>>("/api/courses", input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.all });
    },
  });
}

export function useRemoveStudent(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (studentId: string) => {
      await api.delete(`/api/courses/${courseId}/students/${studentId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.students(courseId) });
    },
  });
}

export function useRegenerateJoinCode(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiEnvelope<Course>>(`/api/courses/${courseId}/join-code/regenerate`);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(courseId) });
    },
  });
}

export interface UpdateCourseInput {
  name: string;
  subject: string;
  description?: string;
}

export function useUpdateCourse(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: UpdateCourseInput) => {
      const res = await api.patch<ApiEnvelope<Course>>(`/api/courses/${courseId}`, input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(courseId) });
      queryClient.invalidateQueries({ queryKey: courseKeys.all });
    },
  });
}

export function useArchiveCourse(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiEnvelope<Course>>(`/api/courses/${courseId}/archive`);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(courseId) });
      queryClient.invalidateQueries({ queryKey: courseKeys.all });
    },
  });
}

export function useUnarchiveCourse(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiEnvelope<Course>>(`/api/courses/${courseId}/unarchive`);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(courseId) });
      queryClient.invalidateQueries({ queryKey: courseKeys.all });
    },
  });
}

export function useJoinCourse() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (code: string) => {
      await api.post("/api/student/courses/join", { code });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.mine });
    },
  });
}
