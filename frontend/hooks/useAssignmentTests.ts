"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { AssignmentTest, TestRunResult } from "@/types/codeRun";

type TestInput = Omit<AssignmentTest, "id" | "stdin" | "expectedOutput"> & { stdin: string; expectedOutput: string };

const tutorKey = (assignmentId: string) => ["assignments", assignmentId, "tests"] as const;
const studentKey = (assignmentId: string) => ["student", "assignments", assignmentId, "tests"] as const;
const resultsKey = (submissionId: string) => ["submissions", submissionId, "test-results"] as const;

export function useAssignmentTests(assignmentId: string) {
  return useQuery({
    queryKey: tutorKey(assignmentId),
    queryFn: async () =>
      (await api.get<ApiEnvelope<AssignmentTest[]>>(`/api/assignments/${assignmentId}/tests`)).data.data,
  });
}

/** What a student may know about the tests: hidden ones have no input or expected output. */
export function useStudentTests(assignmentId: string) {
  return useQuery({
    queryKey: studentKey(assignmentId),
    queryFn: async () =>
      (await api.get<ApiEnvelope<AssignmentTest[]>>(`/api/student/assignments/${assignmentId}/tests`)).data.data,
  });
}

export function useTestMutations(assignmentId: string) {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: tutorKey(assignmentId) });

  const create = useMutation({
    mutationFn: async (test: TestInput) =>
      (await api.post<ApiEnvelope<AssignmentTest>>(`/api/assignments/${assignmentId}/tests`, test)).data.data,
    onSuccess: invalidate,
  });
  const update = useMutation({
    mutationFn: async ({ id, ...test }: TestInput & { id: string }) =>
      (await api.put<ApiEnvelope<AssignmentTest>>(`/api/assignments/${assignmentId}/tests/${id}`, test)).data.data,
    onSuccess: invalidate,
  });
  const remove = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/assignments/${assignmentId}/tests/${id}`);
    },
    onSuccess: invalidate,
  });
  return { create, update, remove };
}

/** A student checks their current code against the tests; nothing is saved. */
export function useCheckCode(assignmentId: string) {
  return useMutation({
    mutationFn: async (code: string) =>
      (await api.post<ApiEnvelope<TestRunResult>>(`/api/assignments/${assignmentId}/check`, { code })).data.data,
  });
}

/** The latest test run on a submission (teachers only); null until it's been run. */
export function useSubmissionTestResults(submissionId: string, enabled: boolean) {
  return useQuery({
    queryKey: resultsKey(submissionId),
    enabled,
    queryFn: async () =>
      (await api.get<ApiEnvelope<TestRunResult | null>>(`/api/submissions/${submissionId}/test-results`)).data.data,
  });
}

export function useRunSubmissionTests(submissionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      (await api.post<ApiEnvelope<TestRunResult>>(`/api/submissions/${submissionId}/run-tests`)).data.data,
    onSuccess: (result) => queryClient.setQueryData(resultsKey(submissionId), result),
  });
}
