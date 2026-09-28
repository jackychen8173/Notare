"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

import { api, type ApiEnvelope } from "@/lib/api";
import { saveSession } from "@/lib/auth";
import type { AuthSession } from "@/types/user";

export type DemoRole = "TUTOR" | "STUDENT";

/** Creates a fresh private demo classroom on the backend and signs in to it. */
export function useStartDemo() {
  const router = useRouter();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (role: DemoRole) => {
      const res = await api.post<ApiEnvelope<AuthSession>>("/api/auth/demo", { role });
      return res.data.data;
    },
    onSuccess: (session) => {
      queryClient.clear();
      saveSession(session);
      router.push(session.role === "TUTOR" ? "/dashboard" : "/student/dashboard");
    },
  });
}
