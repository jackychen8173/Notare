"use client";

import Link from "next/link";
import type { ComponentType } from "react";
import {
  IconArrowBackUp,
  IconChecks,
  IconClipboardText,
  IconClockExclamation,
  IconFileCode,
  IconMessageCircle,
  IconUsers,
} from "@tabler/icons-react";

import { TodayPanel } from "@/components/home/TodayPanel";
import { EmptyState } from "@/components/layout/EmptyState";
import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyCourses } from "@/hooks/useCourses";
import { useStudentHome } from "@/hooks/useHome";
import { useMySessions } from "@/hooks/useSessions";
import { daysUntil, formatClock, isSameDay, relativeDueLabel, timeAgo } from "@/lib/dates";
import { greeting } from "@/lib/greeting";
import { cn } from "@/lib/utils";
import type { HomeCourse, StudentHome } from "@/types/home";
import type { Session } from "@/types/session";

interface NextUp {
  eyebrow: string;
  title: string;
  course?: HomeCourse;
  detail: string;
  action?: { label: string; href: string };
  icon: ComponentType<{ className?: string; stroke?: number }>;
}

const RECENT_FEEDBACK_DAYS = 3;

/**
 * The single most useful thing to do now, in priority order: finish a quiz already started, catch up
 * on overdue work, read feedback that just came back, get ready for today's session, then the next
 * due assignment, then an untaken quiz.
 */
function pickNextUp(home: StudentHome, sessions: Session[]): NextUp | null {
  const now = new Date();

  const inProgress = home.quizzes.find((quiz) => quiz.attemptId);
  if (inProgress) {
    return {
      eyebrow: "Finish your quiz",
      title: inProgress.title,
      course: inProgress.course,
      detail: inProgress.deadlineAt
        ? `You started this quiz. It submits itself at ${formatClock(inProgress.deadlineAt)}.`
        : "You started this quiz and haven't submitted it yet.",
      action: { label: "Continue quiz", href: `/quiz-take/${inProgress.attemptId}` },
      icon: IconClipboardText,
    };
  }

  const overdue = home.assignments.find((assignment) => daysUntil(assignment.dueDate) < 0);
  if (overdue) {
    return {
      eyebrow: "Overdue",
      title: overdue.title,
      course: overdue.course,
      detail: `This was due ${relativeDueLabel(overdue.dueDate).toLowerCase()}. It's not too late to turn it in.`,
      action: { label: "Open assignment", href: `/student/assignments/${overdue.assignmentId}` },
      icon: IconClockExclamation,
    };
  }

  const fresh = home.returned.find(
    (work) => now.getTime() - new Date(work.releasedAt).getTime() < RECENT_FEEDBACK_DAYS * 86_400_000,
  );
  if (fresh) {
    return {
      eyebrow: "Feedback is back",
      title: fresh.title,
      course: fresh.course,
      detail: fresh.grade ? `Your teacher returned this with a grade of ${fresh.grade}.` : "Your teacher returned this with feedback.",
      action: {
        label: "Read feedback",
        href: fresh.kind === "ASSIGNMENT" ? `/student/assignments/${fresh.id}` : `/student/quizzes/${fresh.id}`,
      },
      icon: IconArrowBackUp,
    };
  }

  const sessionToday = sessions
    .filter((s) => s.status === "SCHEDULED" && new Date(s.date) > now && isSameDay(new Date(s.date), now))
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())[0];
  if (sessionToday) {
    return {
      eyebrow: "Session today",
      title: sessionToday.subject,
      detail: `${sessionToday.duration} minutes at ${formatClock(sessionToday.date)}.`,
      icon: IconUsers,
    };
  }

  const nextDue = home.assignments[0];
  if (nextDue && daysUntil(nextDue.dueDate) <= 7) {
    return {
      eyebrow: `Due ${relativeDueLabel(nextDue.dueDate).toLowerCase()}`,
      title: nextDue.title,
      course: nextDue.course,
      detail: "This is the next thing due in your classes.",
      action: { label: "Start working", href: `/student/assignments/${nextDue.assignmentId}` },
      icon: IconFileCode,
    };
  }

  const quiz = home.quizzes[0];
  if (quiz) {
    return {
      eyebrow: "Quiz available",
      title: quiz.title,
      course: quiz.course,
      detail: quiz.timeLimitMinutes ? `You'll have ${quiz.timeLimitMinutes} minutes once you start.` : "There's no time limit.",
      action: { label: "View quiz", href: `/student/quizzes/${quiz.quizId}` },
      icon: IconClipboardText,
    };
  }

  if (nextDue) {
    return {
      eyebrow: `Due ${relativeDueLabel(nextDue.dueDate).toLowerCase()}`,
      title: nextDue.title,
      course: nextDue.course,
      detail: "Nothing is urgent. This is the next thing due.",
      action: { label: "Open assignment", href: `/student/assignments/${nextDue.assignmentId}` },
      icon: IconFileCode,
    };
  }
  return null;
}

function NextUpCard({ next }: { next: NextUp }) {
  const Icon = next.icon;
  return (
    <div
      data-course-color={next.course?.color}
      className="relative overflow-hidden rounded-card border border-border bg-card p-6 shadow-card sm:p-7"
    >
      <div aria-hidden className="absolute inset-y-0 left-0 w-1.5 bg-course" />
      <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 gap-4">
          <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-course-soft text-course">
            <Icon className="size-6" stroke={1.75} aria-hidden />
          </span>
          <div className="min-w-0">
            <p className="text-sm font-medium text-course">{next.eyebrow}</p>
            <p className="mt-0.5 text-xl font-semibold tracking-tight text-foreground sm:text-2xl">{next.title}</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {next.course ? `${next.course.name}. ` : ""}
              {next.detail}
            </p>
          </div>
        </div>
        {next.action ? (
          <Button nativeButton={false} render={<Link href={next.action.href} />} className="h-10 shrink-0 px-5">
            {next.action.label}
          </Button>
        ) : null}
      </div>
    </div>
  );
}

function ListRow({
  href,
  course,
  title,
  meta,
  right,
  urgent,
}: {
  href: string;
  course: HomeCourse;
  title: string;
  meta: string;
  right: string;
  urgent?: boolean;
}) {
  return (
    <li>
      <Link
        href={href}
        data-course-color={course.color}
        className="flex items-center gap-3 px-4 py-3 transition-colors hover:bg-muted/60"
      >
        <span className="size-2.5 shrink-0 rounded-full bg-course" aria-hidden />
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-foreground">{title}</span>
          <span className="block truncate text-xs text-muted-foreground">{meta}</span>
        </span>
        <span className={cn("shrink-0 text-right text-xs font-medium", urgent ? "text-warning" : "text-muted-foreground")}>
          {right}
        </span>
      </Link>
    </li>
  );
}

function ListCard({ children }: { children: React.ReactNode }) {
  return (
    <ul className="divide-y divide-border overflow-hidden rounded-card border border-border bg-card shadow-card">{children}</ul>
  );
}

export default function StudentHomePage() {
  const home = useStudentHome();
  const courses = useMyCourses();
  const sessions = useMySessions();
  const data = home.data;
  const next = data ? pickNextUp(data, sessions.data ?? []) : null;

  const comingUp = data
    ? [
        ...data.assignments.map((a) => ({
          key: `a-${a.assignmentId}`,
          href: `/student/assignments/${a.assignmentId}`,
          course: a.course,
          title: a.title,
          meta: `Assignment in ${a.course.name}`,
          right: relativeDueLabel(a.dueDate),
          urgent: daysUntil(a.dueDate) <= 1,
          sort: a.dueDate,
        })),
        ...data.quizzes.map((q) => ({
          key: `q-${q.quizId}`,
          href: q.attemptId ? `/quiz-take/${q.attemptId}` : `/student/quizzes/${q.quizId}`,
          course: q.course,
          title: q.title,
          meta: `Quiz in ${q.course.name}${q.timeLimitMinutes ? `, ${q.timeLimitMinutes} min` : ""}`,
          right: q.attemptId ? "In progress" : "Open now",
          urgent: !!q.attemptId,
          sort: "",
        })),
      ].sort((a, b) => a.sort.localeCompare(b.sort))
    : [];

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title={greeting()} />

      <section aria-label="Next up">
        {home.isLoading ? (
          <Skeleton className="h-32 w-full rounded-card" />
        ) : next ? (
          <NextUpCard next={next} />
        ) : (
          <EmptyState
            icon={IconChecks}
            title="You're all caught up"
            description="Nothing is due and no feedback is waiting. New work from your classes will show up here."
          />
        )}
      </section>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]">
        <div className="flex min-w-0 flex-col gap-8">
          <section className="flex flex-col gap-3">
            <h2 className="text-lg font-semibold text-foreground">Coming up</h2>
            {home.isLoading ? (
              <Skeleton className="h-40 w-full rounded-card" />
            ) : comingUp.length > 0 ? (
              <ListCard>
                {comingUp.map(({ key, ...item }) => (
                  <ListRow key={key} {...item} />
                ))}
              </ListCard>
            ) : (
              <p className="rounded-lg border border-dashed border-border px-3 py-4 text-sm text-muted-foreground">
                Nothing due in the next month.
              </p>
            )}
          </section>

          {data && data.returned.length > 0 ? (
            <section className="flex flex-col gap-3">
              <h2 className="text-lg font-semibold text-foreground">Returned to you</h2>
              <ListCard>
                {data.returned.map((work) => (
                  <ListRow
                    key={`${work.kind}-${work.id}`}
                    href={work.kind === "ASSIGNMENT" ? `/student/assignments/${work.id}` : `/student/quizzes/${work.id}`}
                    course={work.course}
                    title={work.title}
                    meta={`${work.kind === "ASSIGNMENT" ? "Assignment" : "Quiz"} in ${work.course.name}${work.grade ? `, grade ${work.grade}` : ""}`}
                    right={timeAgo(work.releasedAt)}
                  />
                ))}
              </ListCard>
            </section>
          ) : null}
        </div>

        <div className="flex min-w-0 flex-col gap-8">
          <section className="flex flex-col gap-3">
            <h2 className="text-lg font-semibold text-foreground">Today</h2>
            <TodayPanel
              courses={courses.data ?? []}
              sessions={sessions.data ?? []}
              isLoading={courses.isLoading || sessions.isLoading}
              courseHref={(id) => `/student/courses/${id}`}
              sessionTitle={(session) => `Tutoring: ${session.subject}`}
              calendarHref="/student/calendar"
            />
          </section>

          {data && data.unreadDiscussions.length > 0 ? (
            <section className="flex flex-col gap-3">
              <h2 className="text-lg font-semibold text-foreground">Unread discussions</h2>
              <ListCard>
                {data.unreadDiscussions.slice(0, 5).map((thread) => (
                  <li key={thread.threadId}>
                    <Link
                      href={`/student/discussions/${thread.threadId}`}
                      data-course-color={thread.course.color}
                      className="flex items-center gap-3 px-4 py-3 transition-colors hover:bg-muted/60"
                    >
                      <IconMessageCircle className="size-4 shrink-0 text-course" stroke={1.75} aria-hidden />
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-sm font-medium text-foreground">{thread.title}</span>
                        <span className="block truncate text-xs text-muted-foreground">
                          {thread.visibility === "PRIVATE" ? "Private, with your teacher" : thread.course.name}
                        </span>
                      </span>
                    </Link>
                  </li>
                ))}
              </ListCard>
            </section>
          ) : null}
        </div>
      </div>
    </div>
  );
}
