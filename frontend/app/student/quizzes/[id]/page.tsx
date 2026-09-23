"use client";

import { use } from "react";
import { useRouter } from "next/navigation";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyQuizAttempt, useStartQuizAttempt } from "@/hooks/useQuizAttempts";
import { useMyQuiz } from "@/hooks/useQuizzes";
import type { AttemptStatus } from "@/types/quizAttempt";

const attemptStatusVariant: Record<AttemptStatus, "default" | "secondary"> = {
  IN_PROGRESS: "secondary",
  SUBMITTED: "default",
};

export default function StudentQuizDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const quiz = useMyQuiz(id);
  const attempt = useMyQuizAttempt(id);
  const startAttempt = useStartQuizAttempt(id);
  const currentAttempt = attempt.data;

  if (quiz.isLoading || attempt.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  if (!quiz.data) {
    return <p className="text-sm text-muted-foreground">Quiz not found.</p>;
  }

  function handleStart() {
    startAttempt.mutate(undefined, {
      onSuccess: (started) => router.push(`/quiz-take/${started.id}`),
    });
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-medium text-foreground">{quiz.data.title}</h1>
        <p className="text-sm text-muted-foreground">
          {quiz.data.courseName}
          {quiz.data.timeLimitMinutes ? ` · ${quiz.data.timeLimitMinutes} min, hard cutoff` : " · Untimed"}
          {" · "}
          {quiz.data.totalPointsPossible} pts
        </p>
        {quiz.data.description ? (
          <p className="mt-2 text-sm text-muted-foreground">{quiz.data.description}</p>
        ) : null}
      </div>

      {!currentAttempt ? (
        <Card>
          <CardContent className="flex flex-col gap-3">
            <p className="text-sm text-muted-foreground">
              You get one attempt.
              {quiz.data.timeLimitMinutes
                ? ` The ${quiz.data.timeLimitMinutes}-minute timer starts as soon as you click start, and the quiz auto-submits when it runs out.`
                : ""}
            </p>
            <Button className="self-start" disabled={startAttempt.isPending} onClick={handleStart}>
              {startAttempt.isPending ? "Starting..." : "Start quiz"}
            </Button>
          </CardContent>
        </Card>
      ) : currentAttempt.status === "IN_PROGRESS" ? (
        <Card>
          <CardContent className="flex flex-col gap-3">
            <p className="text-sm text-muted-foreground">You have an attempt in progress.</p>
            <Button className="self-start" onClick={() => router.push(`/quiz-take/${currentAttempt.id}`)}>
              Resume quiz
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {currentAttempt.submittedAt
                ? `Submitted ${new Date(currentAttempt.submittedAt).toLocaleString()}`
                : "Submitted"}
              {currentAttempt.autoSubmitted ? " (auto-submitted at the time limit)" : ""}
            </p>
            <Badge variant={attemptStatusVariant[currentAttempt.status]}>
              {currentAttempt.releasedAt ? "Graded" : "Submitted"}
            </Badge>
          </div>

          {currentAttempt.releasedAt ? (
            <>
              <Card>
                <CardContent>
                  <p className="text-sm font-medium text-foreground">
                    Score: {currentAttempt.totalPointsAwarded ?? 0} / {currentAttempt.totalPointsPossible ?? 0}
                  </p>
                </CardContent>
              </Card>
              {currentAttempt.answers.map((answer, index) => (
                <Card key={answer.questionId}>
                  <CardContent className="flex flex-col gap-2">
                    <p className="text-sm font-medium text-foreground">
                      {index + 1}. {answer.questionPrompt}{" "}
                      <span className="font-normal text-muted-foreground">
                        ({answer.pointsPossible} pts)
                      </span>
                    </p>
                    {answer.questionType === "MULTIPLE_CHOICE" || answer.questionType === "TRUE_FALSE" ? (
                      <ul className="flex flex-col gap-0.5">
                        {answer.options.map((option) => (
                          <li
                            key={option.id}
                            className={
                              option.correct
                                ? "text-sm font-medium text-foreground"
                                : option.id === answer.selectedOptionId
                                  ? "text-sm text-destructive"
                                  : "text-sm text-muted-foreground"
                            }
                          >
                            {option.id === answer.selectedOptionId ? "→ " : option.correct ? "✓ " : "— "}
                            {option.text}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                        {answer.textResponse || "(left blank)"}
                      </p>
                    )}
                    <p className="text-xs text-muted-foreground">
                      {answer.pointsAwarded ?? 0} / {answer.pointsPossible} pts
                    </p>
                    {answer.tutorFeedback ? (
                      <p className="text-sm text-foreground">{answer.tutorFeedback}</p>
                    ) : null}
                  </CardContent>
                </Card>
              ))}
            </>
          ) : (
            <Card>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  Your tutor hasn&apos;t released your results yet.
                </p>
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
