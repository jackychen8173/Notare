"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

import { clearSession } from "@/lib/auth";

/** Clears the stored session and every cached query (so the next user never sees this one's data). */
export function useSignOut() {
  const router = useRouter();
  const queryClient = useQueryClient();
  return (redirectTo = "/") => {
    clearSession();
    queryClient.clear();
    router.replace(redirectTo);
  };
}
