"use client";

import { IconBook2, IconClipboardCheck, IconMessages, IconUsers } from "@tabler/icons-react";

import { CourseCard } from "@/components/course/CourseCard";
import { DueSoonList } from "@/components/dashboard/DueSoonList";
import { StatTile } from "@/components/dashboard/StatTile";
import { EmptyState } from "@/components/layout/EmptyState";
import { PageHeader } from "@/components/layout/PageHeader";
import { Skeleton } from "@/components/ui/skeleton";
import { useAssignmentsForCourses } from "@/hooks/useAssignments";
import { useCourses } from "@/hooks/useCourses";
import { useUnreadDiscussionCounts } from "@/hooks/useDiscussions";
import { useStudents } from "@/hooks/useStudents";
import { usePendingReviews } from "@/hooks/useSubmissions";
import { greeting } from "@/lib/greeting";

export default function DashboardPage() {
  const students = useStudents();
  const courses = useCourses();
  const pendingReviews = usePendingReviews();
  const courseList = courses.data ?? [];
  const courseIds = courseList.map((course) => course.id);
  const assignments = useAssignmentsForCourses(courseIds);
  const unread = useUnreadDiscussionCounts("tutor", courseIds);

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title={greeting()} description="Here's what's happening across your classes." />

      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        <StatTile label="Students" value={students.data?.length} isLoading={students.isLoading} icon={IconUsers} href="/students" />
        <StatTile label="Active courses" value={courseList.length} isLoading={courses.isLoading} icon={IconBook2} href="/courses" />
        <StatTile
          label="Needs grading"
          value={pendingReviews.data?.count}
          isLoading={pendingReviews.isLoading}
          icon={IconClipboardCheck}
        />
        <StatTile label="Unread" value={unread.total} isLoading={courses.isLoading} icon={IconMessages} />
      </div>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
        <section className="flex min-w-0 flex-col gap-3">
          <h2 className="text-lg font-semibold text-foreground">Due this week</h2>
          <DueSoonList
            assignments={assignments.data}
            courses={courseList}
            isLoading={courses.isLoading || assignments.isLoading}
            windowDays={7}
            href={(assignment) => `/assignments/${assignment.id}`}
            emptyTitle="Nothing due this week"
          />
        </section>

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
                    href={`/courses/${course.id}`}
                    footer={
                      count > 0 ? (
                        <span className="font-medium text-primary">
                          {count} unread discussion{count === 1 ? "" : "s"}
                        </span>
                      ) : (
                        "No unread discussions"
                      )
                    }
                  />
                );
              })}
            </div>
          ) : (
            <EmptyState
              icon={IconBook2}
              title="No courses yet"
              description="Create your first course from the Courses page."
            />
          )}
        </section>
      </div>
    </div>
  );
}
