"use client";

import Link from "next/link";
import { IconDownload, IconTable } from "@tabler/icons-react";

import { EmptyState } from "@/components/layout/EmptyState";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useGradebook } from "@/hooks/useGradebook";
import { downloadCsv, toCsv } from "@/lib/csv";
import { formatDueDate } from "@/lib/dates";
import { cn } from "@/lib/utils";
import type { Gradebook as GradebookData, GradebookCell, GradebookColumn } from "@/types/gradebook";

export function formatPercent(value: number | null): string {
  return value == null ? "—" : `${Number(value).toFixed(1)}%`;
}

export function formatPoints(value: number): string {
  // 12 stays "12"; 12.5 stays "12.5"; 12.25 stays "12.25".
  return String(Number(value));
}

/** Why a column doesn't count toward the course grade, or null when it does. */
export function notCountedReason(column: GradebookColumn, hasCategories: boolean): string | null {
  if (column.countsTowardGrade) {
    return null;
  }
  if (column.pointsPossible == null) {
    return column.kind === "QUIZ" ? "No questions yet, so it doesn't count" : "No points, so it doesn't count";
  }
  return hasCategories ? "Not in a grade category, so it doesn't count" : "Doesn't count";
}

/** A cell's text for the CSV: the score, the old text grade, or what's going on. */
function cellCsv(cell: GradebookCell): string | number {
  if (cell.points != null) {
    return Number(cell.points);
  }
  if (cell.grade) {
    return cell.grade;
  }
  switch (cell.status) {
    case "MISSING":
      return "Missing";
    case "SUBMITTED":
      return "Not graded";
    case "IN_PROGRESS":
      return "In progress";
    case "RETURNED":
      return "Returned";
    default:
      return "";
  }
}

function exportCsv(book: GradebookData, courseName: string) {
  const header = [
    "Student",
    "Email",
    "Course grade (%)",
    ...book.categories.map((category) => `${category.name} (%)`),
    ...book.columns.map((column) =>
      column.pointsPossible != null ? `${column.title} (/${formatPoints(column.pointsPossible)})` : column.title,
    ),
  ];
  const rows = book.students.map((student) => [
    student.name,
    student.email,
    student.coursePercent,
    ...student.categoryPercents,
    ...student.cells.map(cellCsv),
  ]);
  // The viewer's own date ("en-CA" is YYYY-MM-DD); toISOString would give tomorrow's in the evening.
  const date = new Date().toLocaleDateString("en-CA");
  const safeName = courseName.replace(/[^\p{L}\p{N}]+/gu, "-").replace(/^-|-$/g, "") || "course";
  downloadCsv(`${safeName}-grades-${date}.csv`, toCsv([header, ...rows]));
}

function columnHref(column: GradebookColumn) {
  return column.kind === "ASSIGNMENT" ? `/assignments/${column.id}` : `/quizzes/${column.id}`;
}

function cellHref(column: GradebookColumn, cell: GradebookCell) {
  if (!cell.workId) {
    return null;
  }
  return column.kind === "ASSIGNMENT" ? `/submissions/${cell.workId}/review` : `/quiz-attempts/${cell.workId}/review`;
}

function lateTitle(cell: GradebookCell) {
  const days = `${cell.daysLate} ${cell.daysLate === 1 ? "day" : "days"} late`;
  if (cell.latePenaltyPercent > 0 && cell.pointsBeforePenalty != null) {
    return `${days}, −${cell.latePenaltyPercent}% (${formatPoints(cell.pointsBeforePenalty)} before the penalty)`;
  }
  return days;
}

function CellContent({ cell }: { cell: GradebookCell }) {
  // A newer version waiting for review, on top of an earlier score.
  const pendingDot =
    cell.status === "SUBMITTED" && (cell.points != null || cell.grade) ? (
      <span className="size-1.5 rounded-full bg-primary" aria-label="New version to review" title="New version to review" />
    ) : null;

  if (cell.points != null) {
    return (
      <span className="inline-flex items-center gap-1.5">
        <span className={cn("font-mono", cell.late ? "text-warning" : "text-foreground")}>
          {formatPoints(cell.points)}
        </span>
        {cell.late ? (
          <span className="font-mono text-[10px] uppercase text-warning" title={lateTitle(cell)} aria-label={lateTitle(cell)}>
            late
          </span>
        ) : null}
        {pendingDot}
      </span>
    );
  }
  if (cell.grade) {
    return (
      <span className="inline-flex items-center gap-1.5">
        <span className="text-foreground" title="Graded before points existed">
          {cell.grade}
        </span>
        {pendingDot}
      </span>
    );
  }
  switch (cell.status) {
    case "MISSING":
      return <span className="text-xs font-medium text-destructive">Missing</span>;
    case "SUBMITTED":
      return (
        <span className={cn("text-xs font-medium", cell.late ? "text-warning" : "text-primary")} title={cell.late ? lateTitle(cell) : undefined}>
          To grade{cell.late ? " · late" : ""}
        </span>
      );
    case "IN_PROGRESS":
      return <span className="text-xs text-muted-foreground">In progress</span>;
    case "RETURNED":
      return <span className="text-xs text-muted-foreground">Returned</span>;
    default:
      return <span className="text-muted-foreground">—</span>;
  }
}

/** The tutor's gradebook: a row per student, a column per assignment and published quiz. */
export function Gradebook({ courseId, courseName }: { courseId: string; courseName: string }) {
  const gradebook = useGradebook(courseId);

  if (gradebook.isLoading) {
    return <Skeleton className="h-48 w-full rounded-card" />;
  }

  const book = gradebook.data;
  if (!book || book.students.length === 0 || book.columns.length === 0) {
    return (
      <EmptyState
        icon={IconTable}
        title="No grades yet"
        description={
          !book || book.students.length === 0
            ? "Grades appear once students join the class."
            : "Post assignments or quizzes with points, and scores show up here as you return work."
        }
      />
    );
  }

  const hasCategories = book.categories.length > 0;
  const categoryName = new Map(book.categories.map((category) => [category.id, category.name]));
  const uncounted = book.columns.filter((column) => !column.countsTowardGrade).length;

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex max-w-2xl flex-col gap-1 text-sm text-muted-foreground">
          <p>
            {hasCategories ? (
              <>
                Course grades are weighted by category:{" "}
                {book.categories.map((category, i) => (
                  <span key={category.id}>
                    {i > 0 ? ", " : ""}
                    <span className="text-foreground">{category.name}</span>{" "}
                    <span className="font-mono">{Number(category.weightPercent)}%</span>
                  </span>
                ))}
                . Categories with nothing graded yet are left out.
              </>
            ) : (
              <>Course grades are total points earned over points possible.</>
            )}{" "}
            Only returned work counts; missing work isn&apos;t counted as zero.
          </p>
          {uncounted > 0 ? (
            <p>
              {uncounted} {uncounted === 1 ? "column doesn't" : "columns don't"} count toward the grade (shown faded).
              {hasCategories ? " Give work points and a category in its settings to count it." : ""}
            </p>
          ) : null}
        </div>
        <Button variant="outline" size="sm" onClick={() => exportCsv(book, courseName)}>
          <IconDownload className="size-4" />
          Export CSV
        </Button>
      </div>

      <Card className="overflow-x-auto py-0">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border align-bottom">
              <th className="sticky left-0 z-10 min-w-44 bg-card px-4 py-2.5 text-left font-medium text-muted-foreground">
                Student
              </th>
              <th className="px-3 py-2.5 text-left font-medium text-foreground">Grade</th>
              {book.columns.map((column) => {
                const reason = notCountedReason(column, hasCategories);
                return (
                  <th
                    key={column.id}
                    className={cn("min-w-28 px-3 py-2.5 text-left font-medium", reason ? "opacity-55" : "")}
                    title={reason ?? undefined}
                  >
                    <Link href={columnHref(column)} className="block max-w-40 truncate text-foreground hover:underline">
                      {column.title}
                    </Link>
                    <span className="block font-mono text-[11px] font-normal text-muted-foreground">
                      {column.kind === "QUIZ" ? "quiz" : column.dueAt ? `due ${formatDueDate(column.dueAt.slice(0, 10))}` : ""}
                      {column.pointsPossible != null ? ` · ${formatPoints(column.pointsPossible)} pts` : ""}
                    </span>
                    {column.categoryId ? (
                      <span className="block max-w-40 truncate text-[11px] font-normal text-muted-foreground">
                        {categoryName.get(column.categoryId)}
                      </span>
                    ) : null}
                    {column.averagePercent != null ? (
                      <span className="block font-mono text-[11px] font-normal text-muted-foreground">
                        avg {formatPercent(column.averagePercent)}
                      </span>
                    ) : null}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {book.students.map((student) => (
              <tr key={student.studentId} className="border-b border-border last:border-0">
                <th scope="row" className="sticky left-0 z-10 bg-card px-4 py-2 text-left font-normal">
                  <span className="block max-w-48 truncate text-foreground">{student.name}</span>
                </th>
                <td
                  className="px-3 py-2 font-mono font-medium text-foreground"
                  title={
                    hasCategories
                      ? book.categories
                          .map((category, i) => `${category.name}: ${formatPercent(student.categoryPercents[i])}`)
                          .join("\n")
                      : undefined
                  }
                >
                  {formatPercent(student.coursePercent)}
                </td>
                {book.columns.map((column, i) => {
                  const cell = student.cells[i];
                  const href = cellHref(column, cell);
                  const content = <CellContent cell={cell} />;
                  return (
                    <td key={column.id} className={cn("px-3 py-2", column.countsTowardGrade ? "" : "opacity-55")}>
                      {href ? (
                        <Link href={href} className="-mx-1.5 rounded px-1.5 py-0.5 hover:bg-muted">
                          {content}
                        </Link>
                      ) : (
                        content
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
      <p className="text-xs text-muted-foreground">
        Scores are after late penalties; hover a late score for the details. A dot means a newer version is waiting for
        review.
      </p>
    </div>
  );
}
