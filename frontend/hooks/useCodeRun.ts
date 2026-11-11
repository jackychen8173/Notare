"use client";

import { useMutation } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { CodeRunResult } from "@/types/codeRun";

async function runCode(assignmentId: string, code: string, stdin: string): Promise<CodeRunResult> {
  const res = await api.post<ApiEnvelope<CodeRunResult>>(`/api/assignments/${assignmentId}/run`, {
    code,
    stdin,
  });
  return res.data.data;
}

export function useRunCode(assignmentId: string) {
  return useMutation({
    mutationFn: ({ code, stdin }: { code: string; stdin: string }) => runCode(assignmentId, code, stdin),
  });
}
