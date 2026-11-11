"use client";

import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { NotificationKind, Notifications } from "@/types/notification";

const notificationsKey = ["notifications"] as const;

const POLL_MS = 2 * 60_000;
/** A tab nobody has touched for this long stops polling, so a forgotten tab doesn't keep the backend awake. */
const IDLE_MS = 5 * 60_000;

let lastActivity = Date.now();

/** Tracks the last time this person used the page (polling stops when they're away). */
export function useActivityTracker() {
  useEffect(() => {
    const touch = () => {
      lastActivity = Date.now();
    };
    const events = ["pointerdown", "keydown", "scroll", "focus"] as const;
    events.forEach((name) => window.addEventListener(name, touch, { passive: true }));
    return () => events.forEach((name) => window.removeEventListener(name, touch));
  }, []);
}

/**
 * The signed-in person's notifications. Refreshes when the window regains focus, and every two
 * minutes while someone is actually using the page.
 */
export function useNotifications() {
  return useQuery({
    queryKey: notificationsKey,
    queryFn: async () => (await api.get<ApiEnvelope<Notifications>>("/api/notifications")).data.data,
    refetchInterval: () => (Date.now() - lastActivity < IDLE_MS ? POLL_MS : false),
    refetchOnWindowFocus: true,
  });
}

/** Everything up to now counts as read. */
export function useMarkNotificationsSeen() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await api.post("/api/notifications/seen");
    },
    onSuccess: () =>
      queryClient.setQueryData<Notifications>(notificationsKey, (current) =>
        current
          ? { ...current, unreadCount: 0, items: current.items.map((item) => ({ ...item, unread: false })) }
          : current,
      ),
  });
}

export function useUpdateNotificationSettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (mutedKinds: NotificationKind[]) =>
      (await api.put<ApiEnvelope<Notifications>>("/api/notifications/settings", { mutedKinds })).data.data,
    // The checkbox flips right away; the list itself refreshes when the server answers.
    onMutate: (mutedKinds) => {
      const previous = queryClient.getQueryData<Notifications>(notificationsKey);
      if (previous) {
        queryClient.setQueryData<Notifications>(notificationsKey, {
          ...previous,
          mutedKinds,
          items: previous.items.filter((item) => !mutedKinds.includes(item.kind)),
        });
      }
      return { previous };
    },
    onError: (_error, _kinds, context) => {
      if (context?.previous) queryClient.setQueryData(notificationsKey, context.previous);
    },
    onSuccess: (data) => queryClient.setQueryData(notificationsKey, data),
  });
}
