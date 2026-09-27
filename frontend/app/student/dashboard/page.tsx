"use client";

import { IconBook2, IconCalendarDue, IconMessages } from "@tabler/icons-react";

import { CourseCard } from "@/components/course/CourseCard";
import { DueSoonList } from "@/components/dashboard/DueSoonList";
import { StatTile } from "@/components/dashboard/StatTile";
import { EmptyState } from "@/components/layout/EmptyState";
import { PageHeader } from "@/components/layout/PageHeader";
import { SessionCard } from "@/components/session/SessionCard";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useAssignmentsForCourses } from "@/hooks/useAssignments";
import { useMyCourses } from "@/hooks/useCourses";
import { useUnreadDiscussionCounts } from "@/hooks/useDiscussions";
import { useMySessions } from "@/hooks/useSessions";
import { useMySubmissionsFor } from "@/hooks/useSubmissions";
import { daysUntil } from "@/lib/dates";
import { greeting } from "@/lib/greeting";

const DUE_WINDOW_DAYS = 14;

export default function StudentDashboardPage() {
  const courses = useMyCourses();
  const sessions = useMySessions();
  const courseList = courses.data ?? [];
  const courseIds = courseList.map((course) => course.id);
  const assignments = useAssignmentsForCourses(courseIds, true);
  const unread = useUnreadDiscussionCounts("student", courseIds);

  const dueSoon = assignments.data.filter((assignment) => {
    const days = daysUntil(assignment.dueDate);
    return days >= 0 && days <= DUE_WINDOW_DAYS;
  });
  const submissions = useMySubmissionsFor(dueSoon.map((assignment) => assignment.id));
  const toDo = dueSoon.filter((assignment) => submissions[assignment.id] === null).length;

  const upcomingSessions = (sessions.data ?? [])
    .filter((s) => s.status === "SCHEDULED")
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
    .slice(0, 3);

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title={greeting()} description="Here's what's coming up in your classes." />

      <div className="grid grid-cols-3 gap-3 sm:gap-4">
        <StatTile label="Courses" value={courseList.length} isLoading={courses.isLoading} icon={IconBook2} href="/student/courses" />
        <StatTile
          label="To do"
          value={toDo}
          isLoading={courses.isLoading || assignments.isLoading}
          icon={IconCalendarDue}
        />
        <StatTile label="Unread" value={unread.total} isLoading={courses.isLoading} icon={IconMessages} />
      </div>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]">
        <section className="flex min-w-0 flex-col gap-3">
          <h2 className="text-lg font-semibold text-foreground">Due soon</h2>
          <DueSoonList
            assignments={assignments.data}
            courses={courseList}
            isLoading={courses.isLoading || assignments.isLoading}
            windowDays={DUE_WINDOW_DAYS}
            href={(assignment) => `/student/assignments/${assignment.id}`}
            status={(assignment) => {
              const submission = submissions[assignment.id];
              if (submission === undefined) return null;
              return submission ? <Badge variant="secondary">Submitted</Badge> : <Badge variant="outline">To do</Badge>;
            }}
            emptyTitle="You're all caught up"
          />
        </section>

        <div className="flex min-w-0 flex-col gap-8">
          <section className="flex min-w-0 flex-col gap-3">
            <h2 className="text-lg font-semibold text-foreground">Your courses</h2>
            {courses.isLoading ? (
              <Skeleton className="h-40 w-full rounded-card" />
            ) : courseList.length > 0 ? (
              <div className="grid gap-4 sm:grid-cols-2">
                {courseList.map((course) => {
                  const count = unread.byCourse[course.id] ?? 0;
                  return (
                    <CourseCard
                      key={course.id}
                      course={course}
                      href={`/student/courses/${course.id}`}
                      meta={course.tutorName}
                      footer={
                        count > 0 ? (
                          <span className="font-medium text-primary">
                            {count} unread discussion{count === 1 ? "" : "s"}
                          </span>
                        ) : null
                      }
                    />
                  );
                })}
              </div>
            ) : (
              <EmptyState
                icon={IconBook2}
                title="No courses yet"
                description="Join a class from the Courses page with the code your tutor gave you."
              />
            )}
          </section>

          {upcomingSessions.length > 0 ? (
            <section className="flex min-w-0 flex-col gap-3">
              <h2 className="text-lg font-semibold text-foreground">Next sessions</h2>
              <div className="flex flex-col gap-3">
                {upcomingSessions.map((session) => (
                  <SessionCard key={session.id} session={session} />
                ))}
              </div>
            </section>
          ) : null}
        </div>
      </div>
    </div>
  );
}
