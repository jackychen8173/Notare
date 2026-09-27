import Link from "next/link";
import type { ReactNode } from "react";
import { IconCalendarCheck } from "@tabler/icons-react";

import { EmptyState } from "@/components/layout/EmptyState";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { daysUntil, relativeDueLabel } from "@/lib/dates";
import { cn } from "@/lib/utils";
import type { Assignment } from "@/types/assignment";
import type { Course } from "@/types/course";

interface DueSoonListProps {
  assignments: Assignment[];
  courses: Course[];
  isLoading: boolean;
  /** Days ahead to include (today counts as day 0). */
  windowDays: number;
  href: (assignment: Assignment) => string;
  /** Optional right-hand status per assignment (e.g. the student's submission state). */
  status?: (assignment: Assignment) => ReactNode;
  emptyTitle: string;
}

export function DueSoonList({
  assignments,
  courses,
  isLoading,
  windowDays,
  href,
  status,
  emptyTitle,
}: DueSoonListProps) {
  if (isLoading) return <Skeleton className="h-40 w-full rounded-card" />;

  const colorByCourse = new Map(courses.map((course) => [course.id, course.color]));
  const due = assignments
    .filter((assignment) => {
      const days = daysUntil(assignment.dueDate);
      return days >= 0 && days <= windowDays;
    })
    .sort((a, b) => daysUntil(a.dueDate) - daysUntil(b.dueDate));

  if (due.length === 0) {
    return <EmptyState icon={IconCalendarCheck} title={emptyTitle} description="Nothing is due in this window." />;
  }

  return (
    <Card className="gap-0 py-0">
      <ul className="divide-y divide-border">
        {due.map((assignment) => {
          const days = daysUntil(assignment.dueDate);
          return (
            <li key={assignment.id}>
              <Link
                href={href(assignment)}
                data-course-color={colorByCourse.get(assignment.courseId)}
                className="flex items-center gap-3 px-4 py-3 transition-colors hover:bg-muted/60"
              >
                <span className="size-2.5 shrink-0 rounded-full bg-course" aria-hidden />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{assignment.title}</p>
                  <p className="truncate text-xs text-muted-foreground">
                    {assignment.courseName}
                    <span className="sm:hidden"> · {relativeDueLabel(assignment.dueDate)}</span>
                  </p>
                </div>
                {status ? <div className="shrink-0">{status(assignment)}</div> : null}
                <span
                  className={cn(
                    "hidden w-20 shrink-0 text-right text-xs font-medium sm:block",
                    days <= 1 ? "text-warning" : "text-muted-foreground",
                  )}
                >
                  {relativeDueLabel(assignment.dueDate)}
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </Card>
  );
}
