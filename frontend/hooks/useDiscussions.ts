"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type {
  CreateThreadInput,
  DiscussionPost,
  DiscussionScope,
  DiscussionThreadDetail,
  DiscussionThreadSummary,
} from "@/types/discussion";

// Discussions are live-ish: poll while a discussion screen is open rather than adding websockets.
const POLL_INTERVAL_MS = 15_000;

export const discussionKeys = {
  forCourse: (scope: DiscussionScope, courseId: string) => [scope, "courses", courseId, "discussions"] as const,
  thread: (scope: DiscussionScope, threadId: string) => [scope, "discussions", threadId] as const,
};

const paths = {
  tutor: {
    course: (courseId: string) => `/api/courses/${courseId}/discussions`,
    thread: (threadId: string) => `/api/discussions/${threadId}`,
    post: (postId: string) => `/api/discussion-posts/${postId}`,
  },
  student: {
    course: (courseId: string) => `/api/student/courses/${courseId}/discussions`,
    thread: (threadId: string) => `/api/student/discussions/${threadId}`,
    post: (postId: string) => `/api/student/discussion-posts/${postId}`,
  },
} as const;

export function useDiscussionThreads(scope: DiscussionScope, courseId: string) {
  return useQuery({
    queryKey: discussionKeys.forCourse(scope, courseId),
    queryFn: async () => {
      const res = await api.get<ApiEnvelope<DiscussionThreadSummary[]>>(paths[scope].course(courseId));
      return res.data.data;
    },
    enabled: !!courseId,
    refetchInterval: POLL_INTERVAL_MS,
  });
}

// Fetching a thread also marks it read server-side, so the course's thread list (unread badges)
// is invalidated whenever a thread load succeeds.
export function useDiscussionThread(scope: DiscussionScope, threadId: string) {
  const queryClient = useQueryClient();
  return useQuery({
    queryKey: discussionKeys.thread(scope, threadId),
    queryFn: async () => {
      const res = await api.get<ApiEnvelope<DiscussionThreadDetail>>(paths[scope].thread(threadId));
      const thread = res.data.data;
      queryClient.invalidateQueries({ queryKey: discussionKeys.forCourse(scope, thread.courseId) });
      return thread;
    },
    enabled: !!threadId,
    refetchInterval: POLL_INTERVAL_MS,
  });
}

export function useCreateDiscussionThread(scope: DiscussionScope, courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateThreadInput) => {
      const res = await api.post<ApiEnvelope<DiscussionThreadDetail>>(paths[scope].course(courseId), input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: discussionKeys.forCourse(scope, courseId) });
    },
  });
}

// Every mutation on an open thread refreshes both the thread and its course list.
function useThreadMutation<TVariables, TResult>(
  scope: DiscussionScope,
  thread: Pick<DiscussionThreadDetail, "id" | "courseId">,
  mutationFn: (variables: TVariables) => Promise<TResult>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: discussionKeys.thread(scope, thread.id) });
      queryClient.invalidateQueries({ queryKey: discussionKeys.forCourse(scope, thread.courseId) });
    },
  });
}

export function useReplyToThread(scope: DiscussionScope, thread: Pick<DiscussionThreadDetail, "id" | "courseId">) {
  return useThreadMutation(scope, thread, async (input: { body: string; anonymous?: boolean }) => {
    const res = await api.post<ApiEnvelope<DiscussionPost>>(`${paths[scope].thread(thread.id)}/posts`, input);
    return res.data.data;
  });
}

export function useUpdateThread(scope: DiscussionScope, thread: Pick<DiscussionThreadDetail, "id" | "courseId">) {
  return useThreadMutation(scope, thread, async (input: { title: string; body: string }) => {
    const res = await api.put<ApiEnvelope<DiscussionThreadDetail>>(paths[scope].thread(thread.id), input);
    return res.data.data;
  });
}

export function useModerateThread(thread: Pick<DiscussionThreadDetail, "id" | "courseId">) {
  return useThreadMutation("tutor", thread, async (input: { pinned?: boolean; locked?: boolean }) => {
    const res = await api.patch<ApiEnvelope<DiscussionThreadDetail>>(`${paths.tutor.thread(thread.id)}/moderation`, input);
    return res.data.data;
  });
}

export function useMakeThreadPublic(thread: Pick<DiscussionThreadDetail, "id" | "courseId">) {
  return useThreadMutation<void, DiscussionThreadDetail>("tutor", thread, async () => {
    const res = await api.post<ApiEnvelope<DiscussionThreadDetail>>(`${paths.tutor.thread(thread.id)}/make-public`);
    return res.data.data;
  });
}

export function useUpdatePost(scope: DiscussionScope, thread: Pick<DiscussionThreadDetail, "id" | "courseId">) {
  return useThreadMutation(scope, thread, async (input: { postId: string; body: string }) => {
    const res = await api.put<ApiEnvelope<DiscussionPost>>(paths[scope].post(input.postId), { body: input.body });
    return res.data.data;
  });
}

export function useDeletePost(scope: DiscussionScope, thread: Pick<DiscussionThreadDetail, "id" | "courseId">) {
  return useThreadMutation(scope, thread, async (postId: string) => {
    await api.delete(paths[scope].post(postId));
  });
}

export function useDeleteThread(scope: DiscussionScope, courseId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (threadId: string) => {
      await api.delete(paths[scope].thread(threadId));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: discussionKeys.forCourse(scope, courseId) });
    },
  });
}
