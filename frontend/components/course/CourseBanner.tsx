import type { ReactNode } from "react";

import { IconClock } from "@tabler/icons-react";

import { formatMeetingTimes } from "@/lib/schedule";
import type { Course } from "@/types/course";

/** Course header in the course's own color. `aside` sits on the right (join code, etc.). */
export function CourseBanner({ course, aside }: { course: Course; aside?: ReactNode }) {
  const meetingTimes = formatMeetingTimes(course.schedule);
  return (
    <div
      data-course-color={course.color}
      className="relative overflow-hidden rounded-card bg-course-solid px-6 py-7 text-white shadow-card sm:px-8"
    >
      {/* Soft highlight so the banner isn't a flat slab; decorative only. */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_85%_-20%,rgb(255_255_255/0.28),transparent_55%)]"
      />
      <div aria-hidden className="pointer-events-none absolute -right-10 -bottom-16 size-48 rounded-full bg-white/10" />
      <div className="relative flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="min-w-0">
          <h1 className="text-2xl font-semibold tracking-tight text-balance sm:text-3xl">{course.name}</h1>
          <p className="mt-1 text-sm text-white/85">
            {course.subject}
            {course.tutorName ? ` · ${course.tutorName}` : ""}
          </p>
          {meetingTimes ? (
            <p className="mt-1 flex items-center gap-1.5 text-sm text-white/85">
              <IconClock className="size-4" stroke={1.75} aria-hidden />
              {meetingTimes}
            </p>
          ) : null}
          {course.description ? <p className="mt-2 max-w-2xl text-sm text-white/80">{course.description}</p> : null}
        </div>
        {aside ? <div className="shrink-0">{aside}</div> : null}
      </div>
    </div>
  );
}
