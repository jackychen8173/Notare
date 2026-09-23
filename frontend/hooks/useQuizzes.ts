"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Quiz, QuestionType } from "@/types/quiz";

export const quizKeys = {
  forCourse: (courseId: string) => ["courses", courseId, "quizzes"] as const,
  detail: (id: string) => ["quizzes", id] as const,
  forMyCourse: (courseId: string) => ["student", "courses", courseId, "quizzes"] as const,
  mineDetail: (id: string) => ["student", "quizzes", id] as const,
};

async function fetchCourseQuizzes(courseId: string): Promise<Quiz[]> {
  const res = await api.get<ApiEnvelope<Quiz[]>>(`/api/courses/${courseId}/quizzes`);
  return res.data.data;
}

async function fetchQuiz(id: string): Promise<Quiz> {
  const res = await api.get<ApiEnvelope<Quiz>>(`/api/quizzes/${id}`);
  return res.data.data;
}

async function fetchMyCourseQuizzes(courseId: string): Promise<Quiz[]> {
  const res = await api.get<ApiEnvelope<Quiz[]>>(`/api/student/courses/${courseId}/quizzes`);
  return res.data.data;
}

async function fetchMyQuiz(id: string): Promise<Quiz> {
  const res = await api.get<ApiEnvelope<Quiz>>(`/api/student/quizzes/${id}`);
  return res.data.data;
}

export function useCourseQuizzes(courseId: string) {
  return useQuery({
    queryKey: quizKeys.forCourse(courseId),
    queryFn: () => fetchCourseQuizzes(courseId),
    enabled: !!courseId,
  });
}

export function useQuiz(id: string) {
  return useQuery({
    queryKey: quizKeys.detail(id),
    queryFn: () => fetchQuiz(id),
    enabled: !!id,
  });
}

export function useMyCourseQuizzes(courseId: string) {
  return useQuery({
    queryKey: quizKeys.forMyCourse(courseId),
    queryFn: () => fetchMyCourseQuizzes(courseId),
    enabled: !!courseId,
  });
}

export function useMyQuiz(id: string) {
  return useQuery({
    queryKey: quizKeys.mineDetail(id),
    queryFn: () => fetchMyQuiz(id),
    enabled: !!id,
  });
}

export interface QuizFormInput {
  title: string;
  description?: string;
  timeLimitMinutes?: number;
  topicId?: string;
  gradeCategoryId?: string;
}

export function useCreateQuiz(courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: QuizFormInput) => {
      const res = await api.post<ApiEnvelope<Quiz>>(`/api/courses/${courseId}/quizzes`, input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: quizKeys.forCourse(courseId) });
    },
  });
}

export function useUpdateQuiz(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: QuizFormInput) => {
      const res = await api.put<ApiEnvelope<Quiz>>(`/api/quizzes/${id}`, input);
      return res.data.data;
    },
    onSuccess: (quiz) => {
      queryClient.setQueryData(quizKeys.detail(id), quiz);
    },
  });
}

export function usePublishQuiz(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.patch<ApiEnvelope<Quiz>>(`/api/quizzes/${id}/publish`);
      return res.data.data;
    },
    onSuccess: (quiz) => {
      queryClient.setQueryData(quizKeys.detail(id), quiz);
    },
  });
}

export function useUnpublishQuiz(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.patch<ApiEnvelope<Quiz>>(`/api/quizzes/${id}/unpublish`);
      return res.data.data;
    },
    onSuccess: (quiz) => {
      queryClient.setQueryData(quizKeys.detail(id), quiz);
    },
  });
}

export interface QuestionOptionInput {
  text: string;
  correct: boolean;
}

export interface QuestionFormInput {
  type: QuestionType;
  prompt: string;
  pointsPossible: number;
  referenceAnswer?: string;
  // MULTIPLE_CHOICE only.
  options?: QuestionOptionInput[];
  // TRUE_FALSE only.
  correctBoolean?: boolean;
}

export function useAddQuestion(quizId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: QuestionFormInput) => {
      const res = await api.post<ApiEnvelope<Quiz>>(`/api/quizzes/${quizId}/questions`, input);
      return res.data.data;
    },
    onSuccess: (quiz) => {
      queryClient.setQueryData(quizKeys.detail(quizId), quiz);
    },
  });
}

export function useUpdateQuestion(quizId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ questionId, input }: { questionId: string; input: QuestionFormInput }) => {
      const res = await api.put<ApiEnvelope<Quiz>>(
        `/api/quizzes/${quizId}/questions/${questionId}`,
        input,
      );
      return res.data.data;
    },
    onSuccess: (quiz) => {
      queryClient.setQueryData(quizKeys.detail(quizId), quiz);
    },
  });
}

export function useDeleteQuestion(quizId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (questionId: string) => {
      const res = await api.delete<ApiEnvelope<Quiz>>(`/api/quizzes/${quizId}/questions/${questionId}`);
      return res.data.data;
    },
    onSuccess: (quiz) => {
      queryClient.setQueryData(quizKeys.detail(quizId), quiz);
    },
  });
}
