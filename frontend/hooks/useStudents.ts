"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Student } from "@/types/user";

export const studentKeys = {
  all: ["students"] as const,
  detail: (id: string) => ["students", id] as const,
};

async function fetchStudents(): Promise<Student[]> {
  const res = await api.get<ApiEnvelope<Student[]>>("/api/students");
  return res.data.data;
}

async function fetchStudent(id: string): Promise<Student> {
  const res = await api.get<ApiEnvelope<Student>>(`/api/students/${id}`);
  return res.data.data;
}

export function useStudents() {
  return useQuery({ queryKey: studentKeys.all, queryFn: fetchStudents });
}

export function useStudent(id: string) {
  return useQuery({
    queryKey: studentKeys.detail(id),
    queryFn: () => fetchStudent(id),
    enabled: !!id,
  });
}

export interface CreateStudentInput {
  name: string;
  email: string;
  password: string;
}

export function useCreateStudent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateStudentInput) => {
      const res = await api.post<ApiEnvelope<Student>>("/api/students", input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studentKeys.all });
    },
  });
}

export interface UpdateStudentInput {
  id: string;
  name: string;
  email: string;
}

export function useUpdateStudent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...input }: UpdateStudentInput) => {
      const res = await api.put<ApiEnvelope<Student>>(`/api/students/${id}`, input);
      return res.data.data;
    },
    onSuccess: (student) => {
      queryClient.invalidateQueries({ queryKey: studentKeys.all });
      queryClient.invalidateQueries({ queryKey: studentKeys.detail(student.id) });
    },
  });
}
