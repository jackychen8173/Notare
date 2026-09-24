import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { Quiz } from "@/types/quiz";

interface QuizCardProps {
  quiz: Quiz;
  /** Defaults to the tutor detail route; pass the student route explicitly there. */
  href?: string;
}

export function QuizCard({ quiz, href }: QuizCardProps) {
  return (
    <Link href={href ?? `/quizzes/${quiz.id}`}>
      <Card className="transition-colors hover:bg-muted/40">
        <CardContent className="flex items-start justify-between gap-3">
          <div>
            <p className="font-medium text-foreground">{quiz.title}</p>
            <p className="text-sm text-muted-foreground">
              {quiz.questions.length} question{quiz.questions.length === 1 ? "" : "s"}
              {quiz.timeLimitMinutes ? ` · ${quiz.timeLimitMinutes} min` : ""}
            </p>
          </div>
          <Badge variant={quiz.publishedAt ? "default" : "secondary"}>
            {quiz.publishedAt ? "Published" : "Draft"}
          </Badge>
        </CardContent>
      </Card>
    </Link>
  );
}
