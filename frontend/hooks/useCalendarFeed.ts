"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";

const calendarFeedKey = ["me", "calendar-feed"] as const;

interface CalendarFeed {
  /** null until the person makes a link. */
  token: string | null;
}

/** The signed-in person's calendar subscribe token. */
export function useCalendarFeed(enabled: boolean) {
  return useQuery({
    queryKey: calendarFeedKey,
    queryFn: async () => (await api.get<ApiEnvelope<CalendarFeed>>("/api/calendar-feed")).data.data,
    enabled,
  });
}

/** Makes the link, or replaces it so the old one stops working. */
export function useResetCalendarFeed() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => (await api.post<ApiEnvelope<CalendarFeed>>("/api/calendar-feed")).data.data,
    onSuccess: (feed) => queryClient.setQueryData(calendarFeedKey, feed),
  });
}

/** The public feed URL, served (and cached) by this site's /api/calendar route. */
export function calendarFeedUrl(token: string): string {
  return `${window.location.origin}/api/calendar/${token}.ics`;
}
