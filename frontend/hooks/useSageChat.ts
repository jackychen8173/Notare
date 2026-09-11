"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { ChatTurnResponse, SageConversationSummary, SageMessage } from "@/types/sageChat";

export const sageChatKeys = {
  conversations: ["sage", "conversations"] as const,
  conversation: (id: string) => ["sage", "conversations", id] as const,
};

async function fetchConversations(): Promise<SageConversationSummary[]> {
  const res = await api.get<ApiEnvelope<SageConversationSummary[]>>("/api/sage/chat");
  return res.data.data;
}

async function fetchConversation(id: string): Promise<SageMessage[]> {
  const res = await api.get<ApiEnvelope<SageMessage[]>>(`/api/sage/chat/${id}`);
  return res.data.data;
}

export function useSageConversations() {
  return useQuery({ queryKey: sageChatKeys.conversations, queryFn: fetchConversations });
}

export function useSageConversation(id: string | null) {
  return useQuery({
    queryKey: sageChatKeys.conversation(id ?? "new"),
    queryFn: () => fetchConversation(id as string),
    enabled: !!id,
  });
}

export function useSendSageMessage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { conversationId: string | null; message: string }) => {
      const res = await api.post<ApiEnvelope<ChatTurnResponse>>("/api/sage/chat", input);
      return res.data.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: sageChatKeys.conversation(data.conversationId) });
      queryClient.invalidateQueries({ queryKey: sageChatKeys.conversations });
    },
  });
}

export function useConfirmSageAction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { conversationId: string; messageId: string }) => {
      const res = await api.post<ApiEnvelope<ChatTurnResponse>>(
        `/api/sage/chat/${input.conversationId}/messages/${input.messageId}/confirm`,
      );
      return res.data.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: sageChatKeys.conversation(data.conversationId) });
    },
  });
}

export function useDeclineSageAction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { conversationId: string; messageId: string }) => {
      const res = await api.post<ApiEnvelope<ChatTurnResponse>>(
        `/api/sage/chat/${input.conversationId}/messages/${input.messageId}/decline`,
      );
      return res.data.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: sageChatKeys.conversation(data.conversationId) });
    },
  });
}
