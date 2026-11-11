"use client";

import dynamic from "next/dynamic";

import { CalendarSubscribeDialog } from "@/components/calendar/CalendarSubscribeDialog";
import type { CalendarLinks } from "@/components/calendar/CalendarView";
import { PageHeader } from "@/components/layout/PageHeader";
import { Skeleton } from "@/components/ui/skeleton";
import { useAssignmentsForCourses } from "@/hooks/useAssignments";
import { useMyCourses } from "@/hooks/useCourses";
import { useQuizzesForCourses } from "@/hooks/useQuizzes";
import { useMySessions } from "@/hooks/useSessions";
import type { Session } from "@/types/session";

// FullCalendar is browser-only and heavy, so it's loaded on the client, in its own chunk.
const CalendarView = dynamic(() => import("@/components/calendar/CalendarView").then((m) => m.CalendarView), {
  ssr: false,
  loading: () => <Skeleton className="h-[36rem] w-full rounded-card" />,
});

// No session link: students have no session detail page.
const links: CalendarLinks = {
  course: (id) => `/student/courses/${id}`,
  assignment: (id) => `/student/assignments/${id}`,
  quiz: (id) => `/student/quizzes/${id}`,
};

const sessionTitle = (session: Session) => `Tutoring: ${session.subject}`;

export default function StudentCalendarPage() {
  const courses = useMyCourses();
  const activeCourses = (courses.data ?? []).filter((course) => !course.archivedAt);
  const assignments = useAssignmentsForCourses(
    activeCourses.map((course) => course.id),
    true,
  );
  const quizzes = useQuizzesForCourses(
    activeCourses.map((course) => course.id),
    true,
  );
  const sessions = useMySessions();

  return (
    <>
      <PageHeader
        title="Calendar"
        description="Your classes, due dates and tutoring sessions."
        actions={<CalendarSubscribeDialog />}
      />
      {courses.isLoading || assignments.isLoading || quizzes.isLoading || sessions.isLoading ? (
        <Skeleton className="h-[36rem] w-full rounded-card" />
      ) : (
        <CalendarView
          courses={activeCourses}
          assignments={assignments.data}
          quizzes={quizzes.data}
          sessions={sessions.data ?? []}
          links={links}
          sessionTitle={sessionTitle}
        />
      )}
    </>
  );
}
