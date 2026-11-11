"use client";

import { useState } from "react";
import Link from "next/link";
import { Popover } from "@base-ui/react/popover";
import {
  IconArrowLeft,
  IconBell,
  IconClockExclamation,
  IconFilePlus,
  IconInbox,
  IconMessageCheck,
  IconMessages,
  IconSettings,
  IconSpeakerphone,
  IconUserPlus,
} from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import {
  useActivityTracker,
  useMarkNotificationsSeen,
  useNotifications,
  useUpdateNotificationSettings,
} from "@/hooks/useNotifications";
import { timeAgo } from "@/lib/dates";
import { cn } from "@/lib/utils";
import type { AppNotification, NotificationKind } from "@/types/notification";

const KINDS: Record<NotificationKind, { label: string; icon: typeof IconBell }> = {
  NEW_WORK: { label: "New assignments and quizzes", icon: IconFilePlus },
  FEEDBACK: { label: "Feedback and quiz scores returned", icon: IconMessageCheck },
  ANNOUNCEMENT: { label: "Announcements", icon: IconSpeakerphone },
  DUE_SOON: { label: "Work due in the next 24 hours", icon: IconClockExclamation },
  DISCUSSION: { label: "Discussion replies and posts", icon: IconMessages },
  SUBMISSION: { label: "Work handed in", icon: IconInbox },
  JOINED: { label: "Students joining a course", icon: IconUserPlus },
};

/** "Due in 5 hours" for due-soon items (their time is when they entered the 24-hour window), else "3 hours ago". */
function when(item: AppNotification): string {
  if (item.kind !== "DUE_SOON") return timeAgo(item.at);
  const hours = Math.max(0, Math.round((new Date(item.at).getTime() + 24 * 3_600_000 - Date.now()) / 3_600_000));
  return hours < 1 ? "Due within the hour" : `Due in ${hours} hour${hours === 1 ? "" : "s"}`;
}

/** The bell in the top bar: what's new across your courses, plus which kinds you want. */
export function NotificationBell() {
  useActivityTracker();
  const [open, setOpen] = useState(false);
  const [settings, setSettings] = useState(false);
  // Which items were unread when the panel opened: they stay highlighted while it's open, even
  // though opening it marks them read.
  const [highlighted, setHighlighted] = useState<Set<string>>(new Set());
  // The muted kinds while a settings change is saving, so the checkbox flips immediately.
  const [pendingMuted, setPendingMuted] = useState<NotificationKind[] | null>(null);
  const notifications = useNotifications();
  const markSeen = useMarkNotificationsSeen();
  const updateSettings = useUpdateNotificationSettings();

  const data = notifications.data;
  const unread = data?.unreadCount ?? 0;

  function onOpenChange(next: boolean) {
    setOpen(next);
    setSettings(false);
    if (next && data) {
      setHighlighted(new Set(data.items.filter((item) => item.unread).map((item) => item.id)));
      if (data.unreadCount > 0) markSeen.mutate();
    }
  }

  function toggleKind(kind: NotificationKind, on: boolean) {
    if (!data) return;
    const current = pendingMuted ?? data.mutedKinds;
    const muted = on ? current.filter((k) => k !== kind) : [...current, kind];
    setPendingMuted(muted);
    updateSettings.mutate(muted, { onSettled: () => setPendingMuted(null) });
  }

  return (
    <Popover.Root open={open} onOpenChange={onOpenChange}>
      <Popover.Trigger
        render={
          <Button
            variant="ghost"
            size="icon"
            className="relative"
            aria-label={unread > 0 ? `Notifications, ${unread} unread` : "Notifications"}
          />
        }
      >
        <IconBell className="size-5" stroke={1.75} />
        {unread > 0 ? (
          <span className="absolute top-1 right-1 flex min-w-4 items-center justify-center rounded-full bg-primary px-1 font-mono text-[10px] leading-4 text-primary-foreground">
            {unread > 9 ? "9+" : unread}
          </span>
        ) : null}
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Positioner className="isolate z-50 outline-none" align="end" sideOffset={6}>
          <Popover.Popup className="flex max-h-[min(36rem,var(--available-height))] w-[min(24rem,calc(100vw-2rem))] origin-(--transform-origin) flex-col overflow-hidden rounded-lg border border-border bg-popover text-popover-foreground shadow-elevated outline-none">
            <div className="flex items-center justify-between gap-2 border-b border-border px-4 py-2.5">
              {settings ? (
                <button
                  type="button"
                  onClick={() => setSettings(false)}
                  className="-ml-1 flex items-center gap-1 rounded px-1 text-sm font-medium text-foreground hover:bg-muted"
                >
                  <IconArrowLeft className="size-4" />
                  Notification settings
                </button>
              ) : (
                <Popover.Title className="text-sm font-semibold text-foreground">Notifications</Popover.Title>
              )}
              {!settings ? (
                <Button variant="ghost" size="icon-sm" aria-label="Notification settings" onClick={() => setSettings(true)}>
                  <IconSettings className="size-4" />
                </Button>
              ) : null}
            </div>

            {settings && data ? (
              <div className="flex flex-col gap-1 overflow-y-auto px-4 py-3">
                <p className="mb-1 text-xs text-muted-foreground">Choose what shows up here.</p>
                {data.availableKinds.map((kind) => (
                  <label key={kind} className="flex items-center gap-2.5 rounded-md px-1 py-1.5 text-sm text-foreground hover:bg-muted">
                    <input
                      type="checkbox"
                      className="accent-primary"
                      checked={!(pendingMuted ?? data.mutedKinds).includes(kind)}
                      onChange={(event) => toggleKind(kind, event.target.checked)}
                    />
                    {KINDS[kind].label}
                  </label>
                ))}
                <p className="mt-2 text-xs text-muted-foreground">Email notifications come later.</p>
              </div>
            ) : notifications.isLoading ? (
              <p className="px-4 py-6 text-sm text-muted-foreground">Loading…</p>
            ) : !data || data.items.length === 0 ? (
              <p className="px-4 py-8 text-center text-sm text-muted-foreground">Nothing new in the last two weeks.</p>
            ) : (
              <ul className="overflow-y-auto py-1">
                {data.items.map((item) => {
                  const Icon = KINDS[item.kind].icon;
                  const isNew = highlighted.has(item.id);
                  return (
                    <li key={item.id}>
                      <Link
                        href={item.href}
                        onClick={() => setOpen(false)}
                        className={cn("flex gap-3 px-4 py-2.5 hover:bg-muted", isNew && "bg-primary-soft/60")}
                      >
                        <Icon
                          className={cn("mt-0.5 size-4 shrink-0", item.kind === "DUE_SOON" ? "text-warning" : "text-muted-foreground")}
                          stroke={1.75}
                          aria-hidden
                        />
                        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                          <span className="text-sm text-foreground">{item.title}</span>
                          {item.detail ? (
                            <span className="line-clamp-2 text-xs text-muted-foreground">{item.detail}</span>
                          ) : null}
                          <span className="font-mono text-[11px] text-muted-foreground">
                            {item.courseName} · {when(item)}
                          </span>
                        </span>
                        {isNew ? <span className="mt-1.5 size-2 shrink-0 rounded-full bg-primary" aria-label="New" /> : null}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            )}
          </Popover.Popup>
        </Popover.Positioner>
      </Popover.Portal>
    </Popover.Root>
  );
}
