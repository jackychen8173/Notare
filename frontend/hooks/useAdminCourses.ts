"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Assignment } from "@/types/assignment";
import type { Course } from "@/types/course";
import type { Student } from "@/types/user";

export const adminCourseKeys = {
  all: ["admin", "courses"] as const,
  detail: (id: string) => ["admin", "courses", id] as const,
  students: (id: string) => ["admin", "courses", id, "students"] as const,
  assignments: (id: string) => ["admin", "courses", id, "assignments"] as const,
};

async function fetchAdminCourses(): Promise<Course[]> {
  const res = await api.get<ApiEnvelope<Course[]>>("/api/admin/courses");
  return res.data.data;
}

async function fetchAdminCourse(id: string): Promise<Course> {
  const res = await api.get<ApiEnvelope<Course>>(`/api/admin/courses/${id}`);
  return res.data.data;
}

async function fetchAdminCourseStudents(id: string): Promise<Student[]> {
  const res = await api.get<ApiEnvelope<Student[]>>(`/api/admin/courses/${id}/students`);
  return res.data.data;
}

async function fetchAdminCourseAssignments(id: string): Promise<Assignment[]> {
  const res = await api.get<ApiEnvelope<Assignment[]>>(`/api/admin/courses/${id}/assignments`);
  return res.data.data;
}

export function useAdminCourses() {
  return useQuery({ queryKey: adminCourseKeys.all, queryFn: fetchAdminCourses });
}

export function useAdminCourse(id: string) {
  return useQuery({
    queryKey: adminCourseKeys.detail(id),
    queryFn: () => fetchAdminCourse(id),
    enabled: !!id,
  });
}

export function useAdminCourseStudents(id: string) {
  return useQuery({
    queryKey: adminCourseKeys.students(id),
    queryFn: () => fetchAdminCourseStudents(id),
    enabled: !!id,
  });
}

export function useAdminCourseAssignments(id: string) {
  return useQuery({
    queryKey: adminCourseKeys.assignments(id),
    queryFn: () => fetchAdminCourseAssignments(id),
    enabled: !!id,
  });
}
