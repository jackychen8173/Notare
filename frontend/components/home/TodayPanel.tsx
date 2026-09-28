import Link from "next/link";
import { IconCalendarEvent } from "@tabler/icons-react";

import { Skeleton } from "@/components/ui/skeleton";
import { formatClock, isSameDay } from "@/lib/dates";
import { formatTime, meetsOn, trimTime } from "@/lib/schedule";
import type { Course } from "@/types/course";
import type { Session } from "@/types/session";

interface TodayItem {
  key: string;
  time: string;
  sortKey: string;
  title: string;
  detail: string;
  color?: Course["color"];
  href?: string;
}

/**
 * Today's class meetings (from each course's weekly schedule) and 1:1 sessions, in time order.
 * When nothing is on today, it says so and points at the next session instead.
 */
export function TodayPanel({
  courses,
  sessions,
  isLoading,
  courseHref,
  sessionHref,
  sessionTitle,
  calendarHref,
}: {
  courses: Course[];
  sessions: Session[];
  isLoading: boolean;
  courseHref: (id: string) => string;
  sessionHref?: (id: string) => string;
  sessionTitle: (session: Session) => string;
  calendarHref: string;
}) {
  if (isLoading) return <Skeleton className="h-32 w-full rounded-card" />;

  const now = new Date();
  const colorOf = new Map(courses.map((course) => [course.id, course.color]));
  const scheduled = sessions.filter((session) => session.status === "SCHEDULED");

  const items: TodayItem[] = [
    ...courses
      .filter((course) => !course.archivedAt && meetsOn(course.schedule, now))
      .map((course) => ({
        key: `meeting-${course.id}`,
        time: `${formatTime(course.schedule!.startTime!)} – ${formatTime(course.schedule!.endTime!)}`,
        sortKey: trimTime(course.schedule!.startTime!),
        title: course.name,
        detail: "Class",
        color: course.color,
        href: courseHref(course.id),
      })),
    ...scheduled
      .filter((session) => isSameDay(new Date(session.date), now))
      .map((session) => {
        const start = new Date(session.date);
        return {
          key: `session-${session.id}`,
          time: formatClock(start),
          sortKey: `${String(start.getHours()).padStart(2, "0")}:${String(start.getMinutes()).padStart(2, "0")}`,
          title: sessionTitle(session),
          detail: `${session.duration} min session`,
          color: session.courseId ? colorOf.get(session.courseId) : undefined,
          href: sessionHref?.(session.id),
        };
      }),
  ].sort((a, b) => a.sortKey.localeCompare(b.sortKey));

  const nextSession = scheduled
    .filter((session) => new Date(session.date) > now && !isSameDay(new Date(session.date), now))
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())[0];

  return (
    <div className="flex flex-col gap-2">
      {items.length > 0 ? (
        <ul className="flex flex-col gap-1.5">
          {items.map((item) => {
            const body = (
              <>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium text-foreground">{item.title}</span>
                  <span className="block text-xs text-muted-foreground">
                    <span className="font-medium text-foreground/80 tabular-nums">{item.time}</span>, {item.detail.toLowerCase()}
                  </span>
                </span>
              </>
            );
            const className =
              "flex items-center gap-3 rounded-lg border border-border bg-card py-2.5 pr-3 pl-3 shadow-[inset_3px_0_0_var(--course)]";
            return (
              <li key={item.key} data-course-color={item.color}>
                {item.href ? (
                  <Link href={item.href} className={`${className} transition-colors hover:bg-muted/60`}>
                    {body}
                  </Link>
                ) : (
                  <div className={className}>{body}</div>
                )}
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="rounded-lg border border-dashed border-border px-3 py-4 text-sm text-muted-foreground">
          No classes or sessions today.
          {/* No trailing period: some locales already end the time with one ("4:00 p.m."). */}
          {nextSession
            ? ` Next session: ${new Date(nextSession.date).toLocaleDateString(undefined, { weekday: "long" })}, ${formatClock(nextSession.date)}`
            : ""}
        </p>
      )}
      <Link
        href={calendarHref}
        className="inline-flex items-center gap-1.5 self-start text-xs font-medium text-primary hover:underline"
      >
        <IconCalendarEvent className="size-3.5" stroke={1.75} aria-hidden />
        Open calendar
      </Link>
    </div>
  );
}
