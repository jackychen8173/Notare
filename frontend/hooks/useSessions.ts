"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Session, SessionNote } from "@/types/session";

export const sessionKeys = {
  all: ["sessions"] as const,
  detail: (id: string) => ["sessions", id] as const,
  notes: (id: string) => ["sessions", id, "notes"] as const,
};

async function fetchSessions(): Promise<Session[]> {
  const res = await api.get<ApiEnvelope<Session[]>>("/api/sessions");
  return res.data.data;
}

async function fetchSession(id: string): Promise<Session> {
  const res = await api.get<ApiEnvelope<Session>>(`/api/sessions/${id}`);
  return res.data.data;
}

async function fetchSessionNotes(id: string): Promise<SessionNote | null> {
  const res = await api.get<ApiEnvelope<SessionNote | null>>(`/api/sessions/${id}/notes`);
  return res.data.data;
}

export function useSessions() {
  return useQuery({ queryKey: sessionKeys.all, queryFn: fetchSessions });
}

export function useSession(id: string) {
  return useQuery({ queryKey: sessionKeys.detail(id), queryFn: () => fetchSession(id), enabled: !!id });
}

export function useSessionNotes(id: string) {
  return useQuery({
    queryKey: sessionKeys.notes(id),
    queryFn: () => fetchSessionNotes(id),
    enabled: !!id,
  });
}

export interface CreateSessionInput {
  studentId: string;
  courseId?: string;
  date: string;
  subject: string;
  duration: number;
}

export function useCreateSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateSessionInput) => {
      const res = await api.post<ApiEnvelope<Session>>("/api/sessions", input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sessionKeys.all });
    },
  });
}

export function useCompleteSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      const res = await api.patch<ApiEnvelope<Session>>(`/api/sessions/${id}/complete`);
      return res.data.data;
    },
    onSuccess: (session) => {
      queryClient.invalidateQueries({ queryKey: sessionKeys.all });
      queryClient.invalidateQueries({ queryKey: sessionKeys.detail(session.id) });
    },
  });
}

export function useSaveSessionNotes(sessionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (rawNotes: string) => {
      const res = await api.post<ApiEnvelope<SessionNote>>(`/api/sessions/${sessionId}/notes`, {
        rawNotes,
      });
      return res.data.data;
    },
    onSuccess: (note) => {
      queryClient.setQueryData(sessionKeys.notes(sessionId), note);
    },
  });
}

export function useDraftSessionNotes(sessionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiEnvelope<SessionNote>>("/api/sage/draft-notes", { sessionId });
      return res.data.data;
    },
    onSuccess: (note) => {
      queryClient.setQueryData(sessionKeys.notes(sessionId), note);
    },
  });
}
