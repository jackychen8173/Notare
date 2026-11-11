"use client";

import Link from "next/link";
import { IconReportAnalytics } from "@tabler/icons-react";

import { LateBadge } from "@/components/assignment/AssignmentTiming";
import { formatPercent, formatPoints, notCountedReason } from "@/components/gradebook/Gradebook";
import { EmptyState } from "@/components/layout/EmptyState";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyGrades } from "@/hooks/useGradebook";
import { formatDueDate } from "@/lib/dates";
import type { GradebookCell, GradebookColumn } from "@/types/gradebook";

function Score({ column, cell }: { column: GradebookColumn; cell: GradebookCell }) {
  if (cell.points != null && column.pointsPossible != null) {
    return (
      <span className="flex flex-col items-end gap-0.5">
        <span className="font-mono text-foreground">
          {formatPoints(cell.points)} / {formatPoints(column.pointsPossible)}
        </span>
        {cell.status === "SUBMITTED" ? (
          <span className="text-xs text-muted-foreground">New version waiting for feedback</span>
        ) : null}
      </span>
    );
  }
  if (cell.grade) {
    return <span className="text-foreground">{cell.grade}</span>;
  }
  switch (cell.status) {
    case "MISSING":
      return <span className="text-sm font-medium text-destructive">Missing</span>;
    case "SUBMITTED":
      return <span className="text-sm text-muted-foreground">Waiting for feedback</span>;
    case "IN_PROGRESS":
      return <span className="text-sm text-muted-foreground">In progress</span>;
    case "RETURNED":
      return <span className="text-sm text-muted-foreground">Returned</span>;
    default:
      return <span className="text-sm text-muted-foreground">Not handed in</span>;
  }
}

/** The signed-in student's grades in a course: the course grade, each category, then every item. */
export function MyGrades({ courseId }: { courseId: string }) {
  const grades = useMyGrades(courseId);

  if (grades.isLoading) {
    return <Skeleton className="h-48 w-full rounded-card" />;
  }

  const data = grades.data;
  if (!data || data.items.length === 0) {
    return (
      <EmptyState
        icon={IconReportAnalytics}
        title="No grades yet"
        description="Once your teacher posts work and returns it with a score, your grades show up here."
      />
    );
  }

  const hasCategories = data.categories.length > 0;
  const categoryName = new Map(data.categories.map((category) => [category.id, category.name]));

  return (
    <div className="flex flex-col gap-4">
      <Card className="gap-4 px-5 py-5">
        <div className="flex flex-wrap items-baseline justify-between gap-3">
          <div>
            <p className="font-mono text-[11px] uppercase tracking-wide text-muted-foreground">Course grade so far</p>
            <p className="text-3xl font-semibold text-foreground">
              {data.coursePercent == null ? "No grades yet" : formatPercent(data.coursePercent)}
            </p>
          </div>
          <p className="max-w-md text-sm text-muted-foreground">
            From work that&apos;s been returned with a score.{" "}
            {hasCategories ? "Each category counts for its share of the grade." : "It's your total points over the points possible."}{" "}
            Work that hasn&apos;t been handed in or returned isn&apos;t counted.
          </p>
        </div>
        {hasCategories ? (
          <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {data.categories.map((category) => (
              <li key={category.id} className="flex items-baseline justify-between gap-3 rounded-lg bg-muted px-3 py-2">
                <span className="min-w-0 truncate text-sm text-foreground">
                  {category.name}{" "}
                  <span className="font-mono text-xs text-muted-foreground">{Number(category.weightPercent)}%</span>
                </span>
                <span className="font-mono text-sm text-foreground">{formatPercent(category.percent)}</span>
              </li>
            ))}
          </ul>
        ) : null}
      </Card>

      <Card className="py-0">
        <ul className="divide-y divide-border">
          {data.items.map(({ column, cell }) => {
            const reason = notCountedReason(column, hasCategories);
            const href = column.kind === "ASSIGNMENT" ? `/student/assignments/${column.id}` : `/student/quizzes/${column.id}`;
            return (
              <li key={column.id} className="flex items-center gap-3 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <Link href={href} className="block truncate text-sm font-medium text-foreground hover:underline">
                    {column.title}
                  </Link>
                  <p className="flex flex-wrap items-center gap-x-2 gap-y-1 font-mono text-[11px] text-muted-foreground">
                    <span className="uppercase">
                      {column.kind === "QUIZ" ? "quiz" : column.dueAt ? `due ${formatDueDate(column.dueAt.slice(0, 10))}` : ""}
                    </span>
                    {column.categoryId ? <span>{categoryName.get(column.categoryId)}</span> : null}
                    {reason ? <span>Doesn&apos;t count toward your grade</span> : null}
                    {cell.late ? <LateBadge daysLate={cell.daysLate} latePenaltyPercent={cell.latePenaltyPercent} /> : null}
                  </p>
                </div>
                <Score column={column} cell={cell} />
              </li>
            );
          })}
        </ul>
      </Card>
    </div>
  );
}
