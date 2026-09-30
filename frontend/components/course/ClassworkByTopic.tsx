import { IconBooks } from "@tabler/icons-react";

import { AssignmentCard } from "@/components/assignment/AssignmentCard";
import { EmptyState } from "@/components/layout/EmptyState";
import { QuizCard } from "@/components/quiz/QuizCard";
import { parseDateOnly } from "@/lib/dates";
import type { Assignment } from "@/types/assignment";
import type { Quiz } from "@/types/quiz";

const UNGROUPED = "No unit";

interface ClassworkByTopicProps {
  assignments: Assignment[];
  quizzes: Quiz[];
  /** Topic names in the tutor's order; topics not listed fall back to first-seen order. */
  topicOrder?: string[];
  assignmentHref?: (assignment: Assignment) => string;
  quizHref?: (quiz: Quiz) => string;
  showQuizStatus?: boolean;
  emptyDescription: string;
}

type Group = { assignments: Assignment[]; quizzes: Quiz[] };

export function ClassworkByTopic({
  assignments,
  quizzes,
  topicOrder = [],
  assignmentHref,
  quizHref,
  showQuizStatus = true,
  emptyDescription,
}: ClassworkByTopicProps) {
  if (assignments.length === 0 && quizzes.length === 0) {
    return <EmptyState icon={IconBooks} title="No classwork yet" description={emptyDescription} />;
  }

  const groups = new Map<string, Group>();
  const group = (name: string | null) => {
    const key = name ?? UNGROUPED;
    let existing = groups.get(key);
    if (!existing) {
      existing = { assignments: [], quizzes: [] };
      groups.set(key, existing);
    }
    return existing;
  };
  [...assignments]
    .sort((a, b) => parseDateOnly(a.dueDate).getTime() - parseDateOnly(b.dueDate).getTime())
    .forEach((assignment) => group(assignment.topicName).assignments.push(assignment));
  quizzes.forEach((quiz) => group(quiz.topicName).quizzes.push(quiz));

  const rank = (name: string) => {
    if (name === UNGROUPED) return Number.MAX_SAFE_INTEGER;
    const index = topicOrder.indexOf(name);
    return index === -1 ? topicOrder.length : index;
  };
  const ordered = [...groups.entries()].sort((a, b) => rank(a[0]) - rank(b[0]));

  return (
    <div className="flex flex-col gap-8">
      {ordered.map(([name, items]) => {
        const count = items.assignments.length + items.quizzes.length;
        return (
          <section key={name}>
            <div className="mb-3 flex items-baseline justify-between gap-3 border-b border-border pb-2">
              <h3 className="text-base font-semibold text-foreground">{name}</h3>
              <p className="text-xs text-muted-foreground">
                {count} item{count === 1 ? "" : "s"}
              </p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {items.assignments.map((assignment) => (
                <AssignmentCard key={assignment.id} assignment={assignment} href={assignmentHref?.(assignment)} />
              ))}
              {items.quizzes.map((quiz) => (
                <QuizCard key={quiz.id} quiz={quiz} href={quizHref?.(quiz)} showStatus={showQuizStatus} />
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}
