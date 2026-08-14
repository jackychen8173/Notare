"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Course } from "@/types/course";
import type { Student } from "@/types/user";

export const courseKeys = {
  all: ["courses"] as const,
  detail: (id: string) => ["courses", id] as const,
  students: (id: string) => ["courses", id, "students"] as const,
  mine: ["student", "courses"] as const,
  mineDetail: (id: string) => ["student", "courses", id] as const,
};

async function fetchCourses(): Promise<Course[]> {
  const res = await api.get<ApiEnvelope<Course[]>>("/api/courses");
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

export function useCourses() {
  return useQuery({ queryKey: courseKeys.all, queryFn: fetchCourses });
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

export function useEnrollStudent(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (studentId: string) => {
      await api.post(`/api/courses/${courseId}/enroll`, { studentId });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.students(courseId) });
    },
  });
}
