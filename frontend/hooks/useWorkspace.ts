"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { CodeRunResult } from "@/types/codeRun";
import type { PracticeFile } from "@/types/workspace";

export const workspaceKeys = {
  files: ["student", "workspace", "files"] as const,
};

export function usePracticeFiles() {
  return useQuery({
    queryKey: workspaceKeys.files,
    queryFn: async () => (await api.get<ApiEnvelope<PracticeFile[]>>("/api/student/workspace/files")).data.data,
  });
}

export interface PracticeFileInput {
  name: string;
  content: string;
}

export function usePracticeFileMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: workspaceKeys.files });

  const create = useMutation({
    mutationFn: async (input: PracticeFileInput) =>
      (await api.post<ApiEnvelope<PracticeFile>>("/api/student/workspace/files", input)).data.data,
    onSuccess: invalidate,
  });
  const save = useMutation({
    mutationFn: async ({ id, ...input }: PracticeFileInput & { id: string }) =>
      (await api.put<ApiEnvelope<PracticeFile>>(`/api/student/workspace/files/${id}`, input)).data.data,
    // Write the saved file straight into the list rather than refetching, so typing isn't disturbed.
    onSuccess: (saved) =>
      queryClient.setQueryData<PracticeFile[]>(workspaceKeys.files, (files) =>
        files?.map((file) => (file.id === saved.id ? saved : file)),
      ),
  });
  const remove = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/student/workspace/files/${id}`);
    },
    onSuccess: invalidate,
  });

  return { create, save, remove };
}

export function useRunPractice() {
  return useMutation({
    mutationFn: async (code: string) =>
      (await api.post<ApiEnvelope<CodeRunResult>>("/api/student/workspace/run", { code })).data.data,
  });
}
