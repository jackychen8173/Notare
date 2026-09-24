"use client";

import { useMutation } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { CodeRunResult } from "@/types/codeRun";

async function runCode(assignmentId: string, code: string): Promise<CodeRunResult> {
  const res = await api.post<ApiEnvelope<CodeRunResult>>(`/api/assignments/${assignmentId}/run`, {
    code,
  });
  return res.data.data;
}

export function useRunCode(assignmentId: string) {
  return useMutation({
    mutationFn: (code: string) => runCode(assignmentId, code),
  });
}
