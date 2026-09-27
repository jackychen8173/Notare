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
