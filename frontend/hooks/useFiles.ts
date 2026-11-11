"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { materialKeys } from "@/hooks/useMaterials";
import { api, type ApiEnvelope } from "@/lib/api";
import type { StoredFileInfo } from "@/types/file";
import type { Material } from "@/types/material";

/** The server's limit, checked here first so a too-big file fails before uploading. */
export const MAX_FILE_BYTES = 10 * 1024 * 1024;

const fileKeys = {
  drafts: (assignmentId: string) => ["student", "assignments", assignmentId, "files"] as const,
  submission: (submissionId: string) => ["submissions", submissionId, "files"] as const,
};

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function form(file: File, fields: Record<string, string | undefined> = {}): FormData {
  const data = new FormData();
  data.append("file", file);
  for (const [key, value] of Object.entries(fields)) {
    if (value) data.append(key, value);
  }
  return data;
}

/**
 * Opens a file. The tab opens right away (inside the click, so pop-up blockers allow it) and is
 * pointed at a short-lived download link once the server hands one over.
 */
export async function openFile(id: string): Promise<void> {
  const tab = window.open("", "_blank");
  try {
    const res = await api.get<ApiEnvelope<{ url: string }>>(`/api/files/${id}/link`);
    // Local development links are paths on the backend; bucket links are absolute.
    const url = new URL(res.data.data.url, api.defaults.baseURL).toString();
    if (tab) {
      tab.opener = null;
      tab.location.href = url;
    } else {
      window.location.assign(url);
    }
  } catch (error) {
    tab?.close();
    throw error;
  }
}

export function useUploadMaterialFile(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { file: File; title: string; description?: string; topicId?: string }) =>
      (
        await api.post<ApiEnvelope<Material>>(
          `/api/courses/${courseId}/materials/file`,
          form(input.file, { title: input.title, description: input.description, topicId: input.topicId }),
        )
      ).data.data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: materialKeys.forCourse(courseId) }),
  });
}

/** Whether uploads are switched on (production needs a storage bucket). Upload buttons hide when they're off. */
export function useUploadsEnabled(): boolean {
  const query = useQuery({
    queryKey: ["files", "enabled"],
    queryFn: async () => (await api.get<ApiEnvelope<{ enabled: boolean }>>("/api/files/enabled")).data.data.enabled,
    staleTime: Infinity,
  });
  return query.data === true;
}

/** The student's files waiting to go in with their next submission. */
export function useDraftFiles(assignmentId: string) {
  return useQuery({
    queryKey: fileKeys.drafts(assignmentId),
    queryFn: async () =>
      (await api.get<ApiEnvelope<StoredFileInfo[]>>(`/api/student/assignments/${assignmentId}/files`)).data.data,
    enabled: !!assignmentId,
  });
}

export function useUploadDraftFile(assignmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (file: File) =>
      (await api.post<ApiEnvelope<StoredFileInfo>>(`/api/student/assignments/${assignmentId}/files`, form(file))).data.data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: fileKeys.drafts(assignmentId) }),
  });
}

export function useDeleteDraftFile(assignmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/files/${id}`);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: fileKeys.drafts(assignmentId) }),
  });
}

/** After submitting, the drafts belong to the submission: refresh both lists. */
export function useRefreshFilesAfterSubmit(assignmentId: string) {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: fileKeys.drafts(assignmentId) });
}

export function useSubmissionFiles(submissionId: string | null | undefined) {
  return useQuery({
    queryKey: fileKeys.submission(submissionId ?? ""),
    queryFn: async () =>
      (await api.get<ApiEnvelope<StoredFileInfo[]>>(`/api/submissions/${submissionId}/files`)).data.data,
    enabled: !!submissionId,
  });
}
