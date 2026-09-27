import Link from "next/link";
import { IconListCheck } from "@tabler/icons-react";

import { Badge } from "@/components/ui/badge";
import type { Quiz } from "@/types/quiz";

interface QuizCardProps {
  quiz: Quiz;
  /** Defaults to the tutor detail route; pass the student route explicitly there. */
  href?: string;
  /** Students never see drafts, so the Published/Draft badge is tutor-only. */
  showStatus?: boolean;
}

export function QuizCard({ quiz, href, showStatus = true }: QuizCardProps) {
  return (
    <Link
      href={href ?? `/quizzes/${quiz.id}`}
      className="group flex items-center gap-3 rounded-card border border-border bg-card p-3 shadow-card transition-all hover:-translate-y-px hover:shadow-elevated"
    >
      <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary-soft text-primary">
        <IconListCheck className="size-[18px]" stroke={1.75} />
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-foreground">{quiz.title}</p>
        <p className="text-xs text-muted-foreground">
          {quiz.questions.length} question{quiz.questions.length === 1 ? "" : "s"}
          {quiz.timeLimitMinutes ? ` · ${quiz.timeLimitMinutes} min` : ""}
        </p>
      </div>
      {showStatus ? (
        <Badge variant={quiz.publishedAt ? "default" : "secondary"}>{quiz.publishedAt ? "Published" : "Draft"}</Badge>
      ) : null}
    </Link>
  );
}
