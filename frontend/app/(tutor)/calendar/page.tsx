"use client";

import dynamic from "next/dynamic";

import type { CalendarLinks } from "@/components/calendar/CalendarView";
import { PageHeader } from "@/components/layout/PageHeader";
import { Skeleton } from "@/components/ui/skeleton";
import { useAssignmentsForCourses } from "@/hooks/useAssignments";
import { useCourses } from "@/hooks/useCourses";
import { useSessions } from "@/hooks/useSessions";
import type { Session } from "@/types/session";

// FullCalendar is browser-only and heavy, so it's loaded on the client, in its own chunk.
const CalendarView = dynamic(() => import("@/components/calendar/CalendarView").then((m) => m.CalendarView), {
  ssr: false,
  loading: () => <Skeleton className="h-[36rem] w-full rounded-card" />,
});

const links: CalendarLinks = {
  course: (id) => `/courses/${id}`,
  assignment: (id) => `/assignments/${id}`,
  session: (id) => `/sessions/${id}`,
};

const sessionTitle = (session: Session) => `Session: ${session.studentName}`;

export default function TutorCalendarPage() {
  const courses = useCourses();
  const courseList = courses.data ?? [];
  const assignments = useAssignmentsForCourses(courseList.map((course) => course.id));
  const sessions = useSessions();
  const hasSchedules = courseList.some((course) => (course.schedule?.days.length ?? 0) > 0);

  return (
    <>
      <PageHeader
        title="Calendar"
        description={
          courses.data && !hasSchedules
            ? "Due dates and sessions across your courses. Add class meeting times from a course's Edit details."
            : "Class meetings, due dates and sessions across your courses."
        }
      />
      {courses.isLoading || assignments.isLoading || sessions.isLoading ? (
        <Skeleton className="h-[36rem] w-full rounded-card" />
      ) : (
        <CalendarView
          courses={courseList}
          assignments={assignments.data}
          sessions={sessions.data ?? []}
          links={links}
          sessionTitle={sessionTitle}
        />
      )}
    </>
  );
}
