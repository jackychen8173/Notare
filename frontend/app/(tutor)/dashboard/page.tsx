"use client";

import { useState } from "react";
import Link from "next/link";
import { IconChecks, IconClipboardText, IconFileCode, IconMessageCircle } from "@tabler/icons-react";

import { DueSoonList } from "@/components/dashboard/DueSoonList";
import { TodayPanel } from "@/components/home/TodayPanel";
import { EmptyState } from "@/components/layout/EmptyState";
import { PageHeader } from "@/components/layout/PageHeader";
import { Skeleton } from "@/components/ui/skeleton";
import { useAssignmentsForCourses } from "@/hooks/useAssignments";
import { useCourses } from "@/hooks/useCourses";
import { useTutorHome } from "@/hooks/useHome";
import { useSessions } from "@/hooks/useSessions";
import { timeAgo } from "@/lib/dates";
import { greeting } from "@/lib/greeting";
import { cn } from "@/lib/utils";
import type { HomeCourse, TutorHome } from "@/types/home";

type Kind = "submission" | "quiz" | "discussion";

interface QueueItem {
  key: string;
  kind: Kind;
  title: string;
  who: string;
  course: HomeCourse;
  at: string | null;
  href: string;
  tag: { label: string; tone: "sage" | "warning" | "muted" | "primary" };
}

const KIND_ICON = { submission: IconFileCode, quiz: IconClipboardText, discussion: IconMessageCircle };

const FILTERS: { value: Kind | null; label: string }[] = [
  { value: null, label: "Everything" },
  { value: "submission", label: "Submissions" },
  { value: "quiz", label: "Quizzes" },
  { value: "discussion", label: "Discussions" },
];

// Work waits oldest-first (whoever has waited longest is on top); discussions follow, newest first.
function toQueue(home: TutorHome): QueueItem[] {
  const work: QueueItem[] = [
    ...home.submissions.map((s) => ({
      key: `s-${s.submissionId}`,
      kind: "submission" as const,
      title: s.attemptNumber > 1 ? `${s.assignmentTitle} · v${s.attemptNumber}` : s.assignmentTitle,
      who: s.attemptNumber > 1 ? `${s.studentName} resubmitted` : s.studentName,
      course: s.course,
      at: s.submittedAt,
      href: `/submissions/${s.submissionId}/review`,
      tag: s.sageDraftReady
        ? { label: "Sage draft ready", tone: "sage" as const }
        : { label: "Not reviewed yet", tone: "muted" as const },
    })),
    ...home.quizAttempts.map((a) => ({
      key: `q-${a.attemptId}`,
      kind: "quiz" as const,
      title: a.quizTitle,
      who: a.studentName,
      course: a.course,
      at: a.submittedAt,
      href: `/quiz-attempts/${a.attemptId}/review`,
      tag: a.needsGrading
        ? { label: "Short answers to grade", tone: "warning" as const }
        : { label: "Graded, ready to release", tone: "muted" as const },
    })),
  ].sort((a, b) => (a.at ?? "").localeCompare(b.at ?? ""));

  const discussions: QueueItem[] = home.unreadDiscussions.map((t) => ({
    key: `d-${t.threadId}`,
    kind: "discussion" as const,
    title: t.title,
    who: t.visibility === "PRIVATE" ? "Private question to you" : "Class discussion",
    course: t.course,
    at: t.lastActivityAt,
    href: `/discussions/${t.threadId}`,
    tag: t.visibility === "PRIVATE" ? { label: "Private", tone: "primary" as const } : { label: "Unread", tone: "muted" as const },
  }));

  return [...work, ...discussions];
}

const TONE_CLASS = {
  sage: "border-sage-border bg-sage-surface text-sage-text",
  warning: "border-transparent bg-warning-surface text-warning",
  primary: "border-transparent bg-primary-soft text-primary",
  muted: "border-border bg-transparent text-muted-foreground",
};

function QueueRow({ item }: { item: QueueItem }) {
  const Icon = KIND_ICON[item.kind];
  return (
    <li>
      <Link
        href={item.href}
        data-course-color={item.course.color}
        className="group flex items-center gap-3 px-4 py-3 transition-colors hover:bg-muted/60"
      >
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-course-soft text-course">
          <Icon className="size-[18px]" stroke={1.75} aria-hidden />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-foreground group-hover:text-primary">{item.title}</span>
          <span className="block truncate text-xs text-muted-foreground">
            {item.who} in {item.course.name}
          </span>
        </span>
        <span className={cn("hidden shrink-0 rounded-full border px-2 py-0.5 text-[11px] font-medium sm:inline", TONE_CLASS[item.tag.tone])}>
          {item.tag.label}
        </span>
        <span className="w-20 shrink-0 text-right font-mono text-[11px] text-muted-foreground">{item.at ? timeAgo(item.at) : ""}</span>
      </Link>
    </li>
  );
}

function summary(home: TutorHome): string {
  const parts: string[] = [];
  const work = home.submissions.length + home.quizAttempts.length;
  if (work > 0) parts.push(`${work} piece${work === 1 ? "" : "s"} of student work`);
  if (home.unreadDiscussions.length > 0) {
    const n = home.unreadDiscussions.length;
    parts.push(`${n} unread discussion${n === 1 ? "" : "s"}`);
  }
  return parts.length > 0 ? `${parts.join(" and ")} waiting on you.` : "Nothing is waiting on you right now.";
}

export default function TutorHomePage() {
  const home = useTutorHome();
  const courses = useCourses();
  const sessions = useSessions();
  const courseList = courses.data ?? [];
  const assignments = useAssignmentsForCourses(courseList.map((course) => course.id));
  const [filter, setFilter] = useState<Kind | null>(null);

  const queue = home.data ? toQueue(home.data) : [];
  const shown = filter ? queue.filter((item) => item.kind === filter) : queue;
  const countOf = (kind: Kind | null) => (kind ? queue.filter((item) => item.kind === kind).length : queue.length);

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title={greeting()} description={home.data ? summary(home.data) : "Checking what needs you..."} />

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
        <section className="flex min-w-0 flex-col gap-3" aria-labelledby="queue-heading">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 id="queue-heading" className="text-lg font-semibold text-foreground">
              To review
            </h2>
            <div className="flex flex-wrap gap-1" role="group" aria-label="Filter the review queue">
              {FILTERS.map((option) => (
                <button
                  key={option.label}
                  type="button"
                  aria-pressed={filter === option.value}
                  onClick={() => setFilter(option.value)}
                  className={cn(
                    "rounded-full px-3 py-1 text-xs font-medium transition-colors",
                    filter === option.value
                      ? "bg-foreground text-background"
                      : "text-muted-foreground hover:bg-muted hover:text-foreground",
                  )}
                >
                  {option.label}
                  <span className="ml-1 tabular-nums opacity-70">{home.data ? countOf(option.value) : ""}</span>
                </button>
              ))}
            </div>
          </div>

          {home.isLoading ? (
            <Skeleton className="h-64 w-full rounded-card" />
          ) : shown.length > 0 ? (
            <ul className="divide-y divide-border overflow-hidden rounded-card border border-border bg-card shadow-card">
              {shown.map((item) => (
                <QueueRow key={item.key} item={item} />
              ))}
            </ul>
          ) : (
            <EmptyState
              icon={IconChecks}
              title="You're all caught up"
              description={
                filter
                  ? "Nothing of this kind is waiting on you."
                  : "New submissions, finished quizzes and unread discussions from every class will show up here."
              }
            />
          )}
        </section>

        <div className="flex min-w-0 flex-col gap-8">
          <section className="flex flex-col gap-3">
            <h2 className="text-lg font-semibold text-foreground">Today</h2>
            <TodayPanel
              courses={courseList}
              sessions={sessions.data ?? []}
              isLoading={courses.isLoading || sessions.isLoading}
              courseHref={(id) => `/courses/${id}`}
              sessionHref={(id) => `/sessions/${id}`}
              sessionTitle={(session) => `Session with ${session.studentName}`}
              calendarHref="/calendar"
            />
          </section>

          <section className="flex flex-col gap-3">
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
        </div>
      </div>
    </div>
  );
}
