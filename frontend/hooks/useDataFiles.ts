"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { DataFile } from "@/types/codeRun";

const tutorKey = (assignmentId: string) => ["assignments", assignmentId, "data-files"] as const;
const studentKey = (assignmentId: string) => ["student", "assignments", assignmentId, "data-files"] as const;

/** The teacher's view of an assignment's data files. */
export function useDataFiles(assignmentId: string) {
  return useQuery({
    queryKey: tutorKey(assignmentId),
    queryFn: async () =>
      (await api.get<ApiEnvelope<DataFile[]>>(`/api/assignments/${assignmentId}/data-files`)).data.data,
  });
}

/** The files a student's Run of this assignment gets, to read or copy from. */
export function useStudentDataFiles(assignmentId: string) {
  return useQuery({
    queryKey: studentKey(assignmentId),
    queryFn: async () =>
      (await api.get<ApiEnvelope<DataFile[]>>(`/api/student/assignments/${assignmentId}/data-files`)).data.data,
  });
}

export function useDataFileMutations(assignmentId: string) {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: tutorKey(assignmentId) });

  const create = useMutation({
    mutationFn: async (file: { name: string; content: string }) =>
      (await api.post<ApiEnvelope<DataFile>>(`/api/assignments/${assignmentId}/data-files`, file)).data.data,
    onSuccess: invalidate,
  });
  const update = useMutation({
    mutationFn: async ({ id, ...file }: { id: string; name: string; content: string }) =>
      (await api.put<ApiEnvelope<DataFile>>(`/api/assignments/${assignmentId}/data-files/${id}`, file)).data.data,
    onSuccess: invalidate,
  });
  const remove = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/assignments/${assignmentId}/data-files/${id}`);
    },
    onSuccess: invalidate,
  });

  return { create, update, remove };
}
