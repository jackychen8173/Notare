"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { QuizAttempt } from "@/types/quizAttempt";

export const quizAttemptKeys = {
  forQuiz: (quizId: string) => ["quizzes", quizId, "attempts"] as const,
  detail: (id: string) => ["quiz-attempts", id] as const,
  mineForQuiz: (quizId: string) => ["student", "quizzes", quizId, "attempt"] as const,
  mineDetail: (id: string) => ["student", "quiz-attempts", id] as const,
};

async function fetchQuizAttempts(quizId: string): Promise<QuizAttempt[]> {
  const res = await api.get<ApiEnvelope<QuizAttempt[]>>(`/api/quizzes/${quizId}/attempts`);
  return res.data.data;
}

async function fetchQuizAttempt(id: string): Promise<QuizAttempt> {
  const res = await api.get<ApiEnvelope<QuizAttempt>>(`/api/quiz-attempts/${id}`);
  return res.data.data;
}

async function fetchMyQuizAttempt(quizId: string): Promise<QuizAttempt | null> {
  const res = await api.get<ApiEnvelope<QuizAttempt | null>>(`/api/student/quizzes/${quizId}/attempt`);
  return res.data.data;
}

async function fetchMyAttemptDetail(id: string): Promise<QuizAttempt> {
  const res = await api.get<ApiEnvelope<QuizAttempt>>(`/api/student/quiz-attempts/${id}`);
  return res.data.data;
}

export function useQuizAttempts(quizId: string) {
  return useQuery({
    queryKey: quizAttemptKeys.forQuiz(quizId),
    queryFn: () => fetchQuizAttempts(quizId),
    enabled: !!quizId,
  });
}

export function useQuizAttempt(id: string) {
  return useQuery({
    queryKey: quizAttemptKeys.detail(id),
    queryFn: () => fetchQuizAttempt(id),
    enabled: !!id,
  });
}

export function useMyQuizAttempt(quizId: string) {
  return useQuery({
    queryKey: quizAttemptKeys.mineForQuiz(quizId),
    queryFn: () => fetchMyQuizAttempt(quizId),
    enabled: !!quizId,
  });
}

/** Used by the full-screen take page to resume/poll a specific in-progress attempt. */
export function useMyAttemptDetail(id: string) {
  return useQuery({
    queryKey: quizAttemptKeys.mineDetail(id),
    queryFn: () => fetchMyAttemptDetail(id),
    enabled: !!id,
  });
}

export function useStartQuizAttempt(quizId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiEnvelope<QuizAttempt>>(`/api/student/quizzes/${quizId}/attempt`);
      return res.data.data;
    },
    onSuccess: (attempt) => {
      queryClient.setQueryData(quizAttemptKeys.mineForQuiz(quizId), attempt);
      queryClient.setQueryData(quizAttemptKeys.mineDetail(attempt.id), attempt);
    },
  });
}

export interface UpsertAnswerInput {
  questionId: string;
  selectedOptionId?: string;
  textResponse?: string;
}

export function useUpsertAnswer(attemptId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ questionId, selectedOptionId, textResponse }: UpsertAnswerInput) => {
      const res = await api.put<ApiEnvelope<QuizAttempt>>(
        `/api/student/quiz-attempts/${attemptId}/answers/${questionId}`,
        { selectedOptionId, textResponse },
      );
      return res.data.data;
    },
    onSuccess: (attempt) => {
      queryClient.setQueryData(quizAttemptKeys.mineDetail(attemptId), attempt);
    },
  });
}

export function useSubmitAttempt(attemptId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiEnvelope<QuizAttempt>>(
        `/api/student/quiz-attempts/${attemptId}/submit`,
      );
      return res.data.data;
    },
    onSuccess: (attempt) => {
      queryClient.setQueryData(quizAttemptKeys.mineDetail(attemptId), attempt);
      queryClient.setQueryData(quizAttemptKeys.mineForQuiz(attempt.quizId), attempt);
    },
  });
}

export interface GradeAnswerInput {
  questionId: string;
  pointsAwarded: number;
  tutorFeedback?: string;
}

export function useGradeAnswer(attemptId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ questionId, pointsAwarded, tutorFeedback }: GradeAnswerInput) => {
      const res = await api.patch<ApiEnvelope<QuizAttempt>>(
        `/api/quiz-attempts/${attemptId}/answers/${questionId}/grade`,
        { pointsAwarded, tutorFeedback },
      );
      return res.data.data;
    },
    onSuccess: (attempt) => {
      queryClient.setQueryData(quizAttemptKeys.detail(attemptId), attempt);
    },
  });
}

export function useReleaseAttempt(attemptId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.patch<ApiEnvelope<QuizAttempt>>(`/api/quiz-attempts/${attemptId}/release`);
      return res.data.data;
    },
    onSuccess: (attempt) => {
      queryClient.setQueryData(quizAttemptKeys.detail(attemptId), attempt);
    },
  });
}

export function useDraftQuizAnswerFeedback(attemptId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (questionId: string) => {
      const res = await api.post<ApiEnvelope<QuizAttempt>>("/api/sage/draft-quiz-answer-feedback", {
        attemptId,
        questionId,
      });
      return res.data.data;
    },
    onSuccess: (attempt) => {
      queryClient.setQueryData(quizAttemptKeys.detail(attemptId), attempt);
    },
  });
}
