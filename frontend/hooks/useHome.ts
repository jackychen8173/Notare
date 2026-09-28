"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { StudentHome, TutorHome } from "@/types/home";

export const homeKeys = {
  tutor: ["home", "tutor"] as const,
  student: ["home", "student"] as const,
};

// The home pages summarize state that many other pages change (releasing feedback, reading a thread),
// so they refetch every time they're opened rather than trusting the 60s default staleTime.
const fresh = { staleTime: 0, refetchOnMount: "always" as const };

export function useTutorHome() {
  return useQuery({
    queryKey: homeKeys.tutor,
    queryFn: async () => (await api.get<ApiEnvelope<TutorHome>>("/api/home")).data.data,
    ...fresh,
  });
}

export function useStudentHome() {
  return useQuery({
    queryKey: homeKeys.student,
    queryFn: async () => (await api.get<ApiEnvelope<StudentHome>>("/api/student/home")).data.data,
    ...fresh,
  });
}
