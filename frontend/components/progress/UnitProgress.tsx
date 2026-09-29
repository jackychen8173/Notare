"use client";

import Link from "next/link";
import { IconCircle, IconCircleCheck, IconCircleDashed, IconClock, IconListCheck } from "@tabler/icons-react";

import { EmptyState } from "@/components/layout/EmptyState";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCourseProgress, useMyProgress } from "@/hooks/useProgress";
import { formatDueDate } from "@/lib/dates";
import { cn } from "@/lib/utils";
import type { ProgressItemStatus } from "@/types/progress";

const STATUS: Record<ProgressItemStatus, { label: string; icon: typeof IconCircle; className: string; segment: string }> = {
  NOT_STARTED: { label: "Not started", icon: IconCircle, className: "text-muted-foreground", segment: "bg-muted" },
  IN_PROGRESS: { label: "In progress", icon: IconCircleDashed, className: "text-warning", segment: "bg-warning/50" },
  SUBMITTED: { label: "Submitted", icon: IconClock, className: "text-primary", segment: "bg-primary/55" },
  RETURNED: { label: "Feedback returned", icon: IconCircleCheck, className: "text-primary", segment: "bg-primary" },
};

function percent(done: number, total: number) {
  return total === 0 ? 0 : Math.round((done / total) * 100);
}

/** The signed-in student's progress through a course, one card per unit. */
export function MyUnitProgress({ courseId }: { courseId: string }) {
  const progress = useMyProgress(courseId);

  if (progress.isLoading) {
    return <Skeleton className="h-48 w-full rounded-card" />;
  }

  const units = progress.data?.units ?? [];
  if (units.length === 0) {
    return (
      <EmptyState
        icon={IconListCheck}
        title="Nothing to track yet"
        description="Once your teacher posts assignments or quizzes, your progress through each unit shows up here."
      />
    );
  }

  const done = units.reduce((sum, unit) => sum + unit.done, 0);
  const total = units.reduce((sum, unit) => sum + unit.total, 0);

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        <span className="font-mono text-foreground">
          {done}/{total}
        </span>{" "}
        done across {units.length} {units.length === 1 ? "unit" : "units"} ({percent(done, total)}%).
      </p>
      {units.map((unit) => (
        <Card key={unit.topicId ?? "none"} className="gap-3 px-4 py-4">
          <div className="flex items-baseline justify-between gap-3">
            <h3 className="font-semibold text-foreground">{unit.name}</h3>
            <span className="font-mono text-xs text-muted-foreground">
              {unit.done}/{unit.total}
            </span>
          </div>
          {unit.total > 0 ? (
            <div className="flex gap-1" aria-hidden>
              {unit.items.map((item) => (
                <span key={item.id} className={cn("h-1.5 flex-1 rounded-full", STATUS[item.status].segment)} />
              ))}
            </div>
          ) : null}
          <ul className="flex flex-col">
            {unit.items.map((item) => {
              const status = STATUS[item.status];
              const Icon = status.icon;
              const href = item.kind === "ASSIGNMENT" ? `/student/assignments/${item.id}` : `/student/quizzes/${item.id}`;
              return (
                <li key={item.id}>
                  <Link
                    href={href}
                    className="-mx-2 flex items-center gap-2.5 rounded-md px-2 py-1.5 text-sm hover:bg-muted"
                  >
                    <Icon className={cn("size-4 shrink-0", status.className)} stroke={1.75} aria-label={status.label} />
                    <span className="min-w-0 flex-1 truncate text-foreground">{item.title}</span>
                    <span className="hidden font-mono text-[11px] uppercase text-muted-foreground sm:inline">
                      {item.kind === "QUIZ" ? "quiz" : item.dueDate ? `due ${formatDueDate(item.dueDate)}` : ""}
                    </span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </Card>
      ))}
    </div>
  );
}

/** The whole class for the tutor: one row per student, one column per unit. */
export function ClassUnitProgress({ courseId }: { courseId: string }) {
  const progress = useCourseProgress(courseId);

  if (progress.isLoading) {
    return <Skeleton className="h-48 w-full rounded-card" />;
  }

  const units = progress.data?.units ?? [];
  const students = progress.data?.students ?? [];
  if (units.length === 0 || students.length === 0) {
    return (
      <EmptyState
        icon={IconListCheck}
        title="Nothing to track yet"
        description={
          students.length === 0
            ? "Progress appears once students join the class."
            : "Post assignments or quizzes and put them in units to see how far each student has gotten."
        }
      />
    );
  }

  const totals = units.map((unit) => unit.assignmentCount + unit.quizCount);

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        How much of each unit every student has turned in. Units come from the Classwork tab; darker cells mean more
        done.
      </p>
      <Card className="overflow-x-auto py-0">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border">
              <th className="sticky left-0 bg-card px-4 py-2.5 text-left font-medium text-muted-foreground">Student</th>
              {units.map((unit, i) => (
                <th key={unit.topicId ?? "none"} className="px-3 py-2.5 text-left font-medium text-foreground">
                  <span className="block max-w-40 truncate">{unit.name}</span>
                  <span className="font-mono text-[11px] font-normal text-muted-foreground">{totals[i]} items</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {students.map((student) => (
              <tr key={student.studentId}>
                <td className="sticky left-0 bg-card px-4 py-2 text-foreground">
                  <Link href={`/students/${student.studentId}`} className="hover:text-primary">
                    {student.name}
                  </Link>
                </td>
                {student.done.map((done, i) => (
                  <td key={i} className="px-3 py-2">
                    <ProgressCell done={done} total={totals[i]} />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="border-t border-border bg-muted/40">
              <td className="sticky left-0 bg-muted/40 px-4 py-2 text-xs font-medium text-muted-foreground">Class</td>
              {units.map((unit, i) => {
                const sum = students.reduce((acc, student) => acc + student.done[i], 0);
                return (
                  <td key={unit.topicId ?? "none"} className="px-3 py-2 font-mono text-xs text-muted-foreground">
                    {percent(sum, totals[i] * students.length)}%
                  </td>
                );
              })}
            </tr>
          </tfoot>
        </table>
      </Card>
    </div>
  );
}

function ProgressCell({ done, total }: { done: number; total: number }) {
  if (total === 0) {
    return <span className="font-mono text-xs text-muted-foreground">—</span>;
  }
  const ratio = done / total;
  return (
    <span
      className={cn(
        "inline-flex min-w-14 items-center justify-center rounded-md px-2 py-0.5 font-mono text-xs",
        ratio === 1
          ? "bg-primary text-primary-foreground"
          : ratio >= 0.5
            ? "bg-primary/35 text-foreground"
            : ratio > 0
              ? "bg-primary/15 text-foreground"
              : "bg-muted text-muted-foreground",
      )}
    >
      {done}/{total}
    </span>
  );
}
