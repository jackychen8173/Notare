"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import FullCalendar from "@fullcalendar/react";
import type { EventClickArg, EventContentArg, EventInput } from "@fullcalendar/core";
import dayGridPlugin from "@fullcalendar/daygrid";
import listPlugin from "@fullcalendar/list";
import timeGridPlugin from "@fullcalendar/timegrid";
import { IconChevronLeft, IconChevronRight } from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import { parseDateOnly } from "@/lib/dates";
import { trimTime, weekdayIndex } from "@/lib/schedule";
import { cn } from "@/lib/utils";
import type { Assignment } from "@/types/assignment";
import type { Course, CourseColor } from "@/types/course";
import type { Session } from "@/types/session";

type EventKind = "meeting" | "assignment" | "session";

const KIND_LABELS: Record<EventKind, string> = {
  meeting: "Class meetings",
  assignment: "Due dates",
  session: "Tutoring sessions",
};

const VIEWS = [
  { id: "dayGridMonth", label: "Month" },
  { id: "timeGridWeek", label: "Week" },
  { id: "listWeek", label: "Agenda" },
] as const;

type ViewId = (typeof VIEWS)[number]["id"];

export interface CalendarLinks {
  course: (id: string) => string;
  assignment: (id: string) => string;
  /** Omitted when this role has no session detail page (students). */
  session?: (id: string) => string;
}

interface CalendarViewProps {
  courses: Course[];
  assignments: Assignment[];
  sessions: Session[];
  links: CalendarLinks;
  /** How a session is titled for this role, e.g. the student's name for a tutor. */
  sessionTitle: (session: Session) => string;
}

/** The day after a date-only string, since FullCalendar treats recurrence/range ends as exclusive. */
function dayAfter(value: string): string {
  const date = parseDateOnly(value);
  date.setDate(date.getDate() + 1);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function buildEvents(
  courses: Course[],
  assignments: Assignment[],
  sessions: Session[],
  links: CalendarLinks,
  sessionTitle: (session: Session) => string,
): EventInput[] {
  const colorOf = new Map<string, CourseColor>(courses.map((course) => [course.id, course.color]));
  const events: EventInput[] = [];

  for (const course of courses) {
    const schedule = course.schedule;
    if (!schedule || schedule.days.length === 0 || !schedule.startTime || !schedule.endTime) continue;
    events.push({
      id: `meeting-${course.id}`,
      groupId: `meeting-${course.id}`,
      title: course.name,
      daysOfWeek: schedule.days.map(weekdayIndex),
      startTime: trimTime(schedule.startTime),
      endTime: trimTime(schedule.endTime),
      startRecur: schedule.termStart ?? undefined,
      endRecur: schedule.termEnd ? dayAfter(schedule.termEnd) : undefined,
      url: links.course(course.id),
      extendedProps: { kind: "meeting", courseId: course.id, color: course.color },
    });
  }

  for (const assignment of assignments) {
    events.push({
      id: `assignment-${assignment.id}`,
      title: `Due: ${assignment.title}`,
      start: assignment.dueDate,
      allDay: true,
      url: links.assignment(assignment.id),
      extendedProps: {
        kind: "assignment",
        courseId: assignment.courseId,
        color: colorOf.get(assignment.courseId),
      },
    });
  }

  for (const session of sessions) {
    if (session.status === "CANCELLED") continue;
    const start = new Date(session.date);
    events.push({
      id: `session-${session.id}`,
      title: sessionTitle(session),
      start,
      end: new Date(start.getTime() + session.duration * 60_000),
      url: links.session?.(session.id),
      extendedProps: {
        kind: "session",
        courseId: session.courseId,
        color: session.courseId ? colorOf.get(session.courseId) : undefined,
      },
    });
  }

  return events;
}

function FilterChip({
  pressed,
  onClick,
  color,
  children,
}: {
  pressed: boolean;
  onClick: () => void;
  color?: CourseColor;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-pressed={pressed}
      onClick={onClick}
      data-course-color={color}
      className={cn(
        "inline-flex h-7 items-center gap-1.5 rounded-full border px-3 text-xs font-medium transition-colors outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
        pressed
          ? "border-border bg-card text-foreground shadow-xs"
          : "border-dashed border-border bg-transparent text-muted-foreground line-through decoration-muted-foreground/60",
      )}
    >
      {color ? <span aria-hidden className={cn("size-2 rounded-full bg-course", !pressed && "opacity-40")} /> : null}
      {children}
    </button>
  );
}

export function CalendarView({ courses, assignments, sessions, links, sessionTitle }: CalendarViewProps) {
  const router = useRouter();
  const calendarRef = useRef<FullCalendar>(null);
  const [title, setTitle] = useState("");
  const [view, setView] = useState<ViewId>("dayGridMonth");
  const [hiddenCourses, setHiddenCourses] = useState<Set<string>>(new Set());
  const [hiddenKinds, setHiddenKinds] = useState<Set<EventKind>>(new Set());

  // Month cells are too narrow to read on a phone; start there on the agenda instead.
  useEffect(() => {
    if (window.matchMedia("(max-width: 640px)").matches) {
      calendarRef.current?.getApi().changeView("listWeek");
    }
  }, []);

  const events = useMemo(() => {
    return buildEvents(courses, assignments, sessions, links, sessionTitle).filter((event) => {
      const { kind, courseId } = event.extendedProps as { kind: EventKind; courseId: string | null };
      if (hiddenKinds.has(kind)) return false;
      return !courseId || !hiddenCourses.has(courseId);
    });
  }, [courses, assignments, sessions, links, sessionTitle, hiddenCourses, hiddenKinds]);

  function toggle<T>(set: Set<T>, value: T): Set<T> {
    const next = new Set(set);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    return next;
  }

  function api() {
    return calendarRef.current?.getApi();
  }

  // Styling hooks for globals.css: the event's kind, and its course color (the `course-color-*`
  // classes set the same `--course` variables as `data-course-color`).
  function eventClassNames({ event }: EventContentArg) {
    const { kind, color } = event.extendedProps as { kind: EventKind; color?: CourseColor };
    return ["nt-event", `nt-event-${kind}`, color ? `course-color-${color}` : "nt-event-no-course"];
  }

  // Client-side navigation instead of FullCalendar's full-page load of `url`.
  function onEventClick({ event, jsEvent }: EventClickArg) {
    if (!event.url) return;
    jsEvent.preventDefault();
    router.push(event.url);
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Button variant="outline" size="icon" aria-label="Previous" onClick={() => api()?.prev()}>
            <IconChevronLeft stroke={1.75} />
          </Button>
          <Button variant="outline" size="icon" aria-label="Next" onClick={() => api()?.next()}>
            <IconChevronRight stroke={1.75} />
          </Button>
          <Button variant="outline" onClick={() => api()?.today()}>
            Today
          </Button>
          <h2 className="ml-1 text-lg font-semibold text-foreground" aria-live="polite">
            {title}
          </h2>
        </div>
        <div className="inline-flex rounded-lg bg-muted p-1" role="group" aria-label="Calendar view">
          {VIEWS.map((option) => (
            <button
              key={option.id}
              type="button"
              aria-pressed={view === option.id}
              onClick={() => api()?.changeView(option.id)}
              className={
                view === option.id
                  ? "rounded-md bg-card px-3 py-1 text-sm font-medium text-foreground shadow-xs"
                  : "rounded-md px-3 py-1 text-sm font-medium text-muted-foreground hover:text-foreground"
              }
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      <div className="flex flex-col gap-2">
        {courses.length > 0 ? (
          <div className="flex flex-wrap gap-1.5" role="group" aria-label="Show courses">
            {courses.map((course) => (
              <FilterChip
                key={course.id}
                color={course.color}
                pressed={!hiddenCourses.has(course.id)}
                onClick={() => setHiddenCourses((prev) => toggle(prev, course.id))}
              >
                {course.name}
              </FilterChip>
            ))}
          </div>
        ) : null}
        <div className="flex flex-wrap gap-1.5" role="group" aria-label="Show event types">
          {(Object.keys(KIND_LABELS) as EventKind[]).map((kind) => (
            <FilterChip
              key={kind}
              pressed={!hiddenKinds.has(kind)}
              onClick={() => setHiddenKinds((prev) => toggle(prev, kind))}
            >
              {KIND_LABELS[kind]}
            </FilterChip>
          ))}
        </div>
      </div>

      <div className="notare-calendar overflow-hidden rounded-card border border-border bg-card p-2 shadow-card sm:p-3">
        <FullCalendar
          ref={calendarRef}
          plugins={[dayGridPlugin, timeGridPlugin, listPlugin]}
          initialView="dayGridMonth"
          headerToolbar={false}
          height="auto"
          events={events}
          eventDisplay="block"
          dayMaxEvents={4}
          nowIndicator
          slotMinTime="07:00:00"
          slotMaxTime="22:00:00"
          scrollTime="08:00:00"
          eventTimeFormat={{ hour: "numeric", minute: "2-digit", meridiem: "short" }}
          // Month cells are narrow, so a short "9a" leaves room for the course name.
          views={{ dayGridMonth: { eventTimeFormat: { hour: "numeric", minute: "2-digit", omitZeroMinute: true, meridiem: "narrow" } } }}
          noEventsContent="Nothing scheduled this week."
          datesSet={(arg) => {
            setTitle(arg.view.title);
            setView(arg.view.type as ViewId);
          }}
          eventClassNames={eventClassNames}
          eventClick={onEventClick}
        />
      </div>
    </div>
  );
}
