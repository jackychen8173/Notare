// Due dates come from the backend as date-only strings ("2026-10-15"). `new Date("2026-10-15")`
// parses that as UTC midnight, which displays as the previous day anywhere west of UTC, so parse
// the parts as a local date instead.
export function parseDateOnly(value: string): Date {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

export function formatDueDate(value: string): string {
  return parseDateOnly(value).toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });
}

/** Whole days from today until the date: 0 = today, negative = past. */
export function daysUntil(value: string): number {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((parseDateOnly(value).getTime() - today.getTime()) / 86_400_000);
}

export function relativeDueLabel(value: string): string {
  const days = daysUntil(value);
  if (days < -1) return `${-days} days ago`;
  if (days === -1) return "Yesterday";
  if (days === 0) return "Today";
  if (days === 1) return "Tomorrow";
  if (days < 7) return `In ${days} days`;
  return formatDueDate(value);
}

/** "just now", "5 min ago", "3 hours ago", "2 days ago", then a date. For timestamps (not date-only strings). */
export function timeAgo(value: string): string {
  const minutes = Math.round((Date.now() - new Date(value).getTime()) / 60_000);
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? "" : "s"} ago`;
  const days = Math.round(hours / 24);
  if (days < 7) return `${days} day${days === 1 ? "" : "s"} ago`;
  return new Date(value).toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

/** "3:30 PM" for a timestamp. */
export function formatClock(value: string | Date): string {
  return new Date(value).toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
}

export function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}
