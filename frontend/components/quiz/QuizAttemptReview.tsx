"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useDraftQuizAnswerFeedback, useGradeAnswer, useReleaseAttempt } from "@/hooks/useQuizAttempts";
import { cn } from "@/lib/utils";
import type { AttemptStatus, QuizAnswer, QuizAttempt } from "@/types/quizAttempt";

const attemptStatusVariant: Record<AttemptStatus, "default" | "secondary"> = {
  IN_PROGRESS: "secondary",
  SUBMITTED: "default",
};

interface SageQuizSuggestion {
  suggestedScore: number;
  feedback: string;
}

function SageSuggestionBlock({ suggestionJson }: { suggestionJson: string }) {
  let suggestion: SageQuizSuggestion | null = null;
  try {
    suggestion = JSON.parse(suggestionJson) as SageQuizSuggestion;
  } catch {
    suggestion = null;
  }

  if (!suggestion) {
    return <p className="text-sm text-destructive">Sage&apos;s suggestion couldn&apos;t be read.</p>;
  }

  return (
    <div className="flex flex-col gap-2 rounded-card border-hairline border-sage-border bg-sage-surface p-4">
      <p className="text-xs font-medium text-sage-text">Sage&apos;s suggestion</p>
      <p className="text-sm font-medium text-sage-text">Suggested score: {suggestion.suggestedScore}</p>
      <p className="text-sm text-sage-text">{suggestion.feedback}</p>
    </div>
  );
}

function ObjectiveAnswerReview({ answer }: { answer: QuizAnswer }) {
  return (
    <ul className="flex flex-col gap-0.5">
      {answer.options.map((option) => {
        const picked = option.id === answer.selectedOptionId;
        return (
          <li
            key={option.id}
            className={cn(
              "text-sm",
              option.correct ? "font-medium text-foreground" : "text-muted-foreground",
              picked && !option.correct ? "text-destructive" : "",
            )}
          >
            {picked ? "→ " : option.correct ? "✓ " : "— "}
            {option.text}
            {picked ? " (student's answer)" : ""}
          </li>
        );
      })}
      {answer.selectedOptionId === null ? (
        <li className="text-sm text-muted-foreground">(not answered)</li>
      ) : null}
    </ul>
  );
}

function FreeTextAnswerReview({ attemptId, answer }: { attemptId: string; answer: QuizAnswer }) {
  const [points, setPoints] = useState(answer.pointsAwarded != null ? String(answer.pointsAwarded) : "");
  const [feedback, setFeedback] = useState(answer.tutorFeedback ?? "");
  const gradeAnswer = useGradeAnswer(attemptId);
  const draftFeedback = useDraftQuizAnswerFeedback(attemptId);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-1.5">
        <Label>Student&apos;s answer</Label>
        <p className="whitespace-pre-wrap rounded-card border-hairline border-border bg-muted/40 p-3 text-sm text-foreground">
          {answer.textResponse || "(left blank)"}
        </p>
      </div>

      {answer.sageSuggestion ? (
        <SageSuggestionBlock suggestionJson={answer.sageSuggestion} />
      ) : (
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="self-start"
          disabled={draftFeedback.isPending}
          onClick={() => draftFeedback.mutate(answer.questionId)}
        >
          {draftFeedback.isPending ? "Asking Sage..." : "Get Sage suggestion"}
        </Button>
      )}

      <div className="flex items-end gap-2">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`points-${answer.questionId}`}>Points</Label>
          <Input
            id={`points-${answer.questionId}`}
            type="number"
            min={0}
            max={answer.pointsPossible}
            step="0.5"
            className="w-24"
            value={points}
            onChange={(event) => setPoints(event.target.value)}
          />
        </div>
        <span className="pb-2 text-xs text-muted-foreground">/ {answer.pointsPossible}</span>
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`feedback-${answer.questionId}`}>Feedback (optional)</Label>
        <Textarea
          id={`feedback-${answer.questionId}`}
          rows={2}
          value={feedback}
          onChange={(event) => setFeedback(event.target.value)}
        />
      </div>
      <Button
        type="button"
        size="sm"
        className="self-start"
        disabled={gradeAnswer.isPending || points === ""}
        onClick={() =>
          gradeAnswer.mutate({
            questionId: answer.questionId,
            pointsAwarded: Number(points),
            tutorFeedback: feedback.trim() || undefined,
          })
        }
      >
        {gradeAnswer.isPending ? "Saving..." : "Save score"}
      </Button>
    </div>
  );
}

interface QuizAttemptReviewProps {
  attempt: QuizAttempt;
}

export function QuizAttemptReview({ attempt }: QuizAttemptReviewProps) {
  const releaseAttempt = useReleaseAttempt(attempt.id);
  const isReleased = attempt.releasedAt != null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-muted-foreground">{attempt.quizTitle}</p>
          <p className="font-medium text-foreground">{attempt.studentName}</p>
        </div>
        <div className="flex items-center gap-3">
          {attempt.totalPointsAwarded != null ? (
            <span className="text-sm text-muted-foreground">
              {attempt.totalPointsAwarded} / {attempt.totalPointsPossible}
            </span>
          ) : null}
          <Badge variant={attemptStatusVariant[attempt.status]}>
            {attempt.autoSubmitted ? "Auto-submitted" : attempt.status}
          </Badge>
        </div>
      </div>

      {attempt.status !== "SUBMITTED" ? (
        <p className="text-sm text-muted-foreground">
          This attempt is still in progress — nothing to grade or release yet.
        </p>
      ) : null}

      <div className="flex flex-col gap-4">
        {attempt.answers.map((answer, index) => (
          <Card key={answer.questionId}>
            <CardContent className="flex flex-col gap-3">
              <p className="text-sm font-medium text-foreground">
                {index + 1}. {answer.questionPrompt}{" "}
                <span className="font-normal text-muted-foreground">({answer.pointsPossible} pts)</span>
              </p>
              {answer.questionType === "MULTIPLE_CHOICE" || answer.questionType === "TRUE_FALSE" ? (
                <ObjectiveAnswerReview answer={answer} />
              ) : (
                <FreeTextAnswerReview attemptId={attempt.id} answer={answer} />
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      <Button
        type="button"
        disabled={releaseAttempt.isPending || attempt.status !== "SUBMITTED"}
        onClick={() => releaseAttempt.mutate()}
        className="self-start"
      >
        {releaseAttempt.isPending
          ? "Releasing..."
          : isReleased
            ? "Update and re-release"
            : "Release to student"}
      </Button>
    </div>
  );
}
