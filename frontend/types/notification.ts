export type NotificationKind =
  | "NEW_WORK"
  | "FEEDBACK"
  | "ANNOUNCEMENT"
  | "DUE_SOON"
  | "DISCUSSION"
  | "SUBMISSION"
  | "JOINED";

export interface AppNotification {
  /** Stable across reads. */
  id: string;
  kind: NotificationKind;
  title: string;
  detail: string | null;
  courseId: string;
  courseName: string;
  /** A path in this app. */
  href: string;
  /** ISO instant. */
  at: string;
  unread: boolean;
}

export interface Notifications {
  items: AppNotification[];
  unreadCount: number;
  /** What this person's role gets, for the settings list. */
  availableKinds: NotificationKind[];
  mutedKinds: NotificationKind[];
}
