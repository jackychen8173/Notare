"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { UserProfile } from "@/types/user";

export const profileKeys = {
  me: ["profile", "me"] as const,
};

async function fetchProfile(): Promise<UserProfile> {
  const res = await api.get<ApiEnvelope<UserProfile>>("/api/users/me");
  return res.data.data;
}

export function useProfile() {
  return useQuery({ queryKey: profileKeys.me, queryFn: fetchProfile });
}

export interface UpdateProfileInput {
  name: string;
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: UpdateProfileInput) => {
      const res = await api.patch<ApiEnvelope<UserProfile>>("/api/users/me", input);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: profileKeys.me });
    },
  });
}
