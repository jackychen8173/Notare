"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useMyAttemptDetail, useSubmitAttempt, useUpsertAnswer } from "@/hooks/useQuizAttempts";

const AUTOSAVE_DEBOUNCE_MS = 700;

function formatRemaining(seconds: number): string {
  const clamped = Math.max(0, seconds);
  const minutes = Math.floor(clamped / 60);
  const secs = clamped % 60;
  return `${minutes}:${String(secs).padStart(2, "0")}`;
}

interface LocalAnswer {
  selectedOptionId?: string;
  textResponse?: string;
}

interface QuizTakeFormProps {
  attemptId: string;
}

export function QuizTakeForm({ attemptId }: QuizTakeFormProps) {
  const router = useRouter();
  const attempt = useMyAttemptDetail(attemptId);
  const upsertAnswer = useUpsertAnswer(attemptId);
  const submitAttempt = useSubmitAttempt(attemptId);

  const [localAnswers, setLocalAnswers] = useState<Record<string, LocalAnswer>>({});
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const seededRef = useRef(false);
  const autoSubmittedRef = useRef(false);
  const debounceTimers = useRef<Record<string, ReturnType<typeof setTimeout>>>({});

  const data = attempt.data;
  const isInProgress = data?.status === "IN_PROGRESS";

  // Seed local form state from the server once, on first load - after that, local state (plus its
  // own autosave calls) is the source of truth for what's on screen, so a background refetch
  // triggered by finalizeIfExpired elsewhere doesn't clobber what the student is mid-typing.
  useEffect(() => {
    if (data && !seededRef.current) {
      seededRef.current = true;
      const seeded: Record<string, LocalAnswer> = {};
      for (const answer of data.answers) {
        seeded[answer.questionId] = {
          selectedOptionId: answer.selectedOptionId ?? undefined,
          textResponse: answer.textResponse ?? undefined,
        };
      }
      setLocalAnswers(seeded);
    }
  }, [data]);

  // React Compiler auto-memoizes derived values and event handlers in this codebase (no other
  // component here reaches for useMemo/useCallback by hand) - a manual useMemo here fought the
  // compiler's own dependency inference (data?.x vs data.x after the null check), so this is a
  // plain computation instead.
  const totalSeconds = data?.deadlineAt
    ? Math.max(1, Math.round((new Date(data.deadlineAt).getTime() - new Date(data.startedAt).getTime()) / 1000))
    : null;

  const submitNow = useCallback(() => {
    if (autoSubmittedRef.current) return;
    autoSubmittedRef.current = true;
    submitAttempt.mutate(undefined, {
      onSuccess: (submitted) => router.push(`/student/quizzes/${submitted.quizId}`),
    });
  }, [submitAttempt, router]);

  // Countdown tick + hard auto-submit at zero. The server independently enforces the same deadline
  // (finalizeIfExpired runs on every touch of this attempt), so a stalled tab or clock skew here
  // can't extend the real deadline - this timer is UX, not the source of truth.
  useEffect(() => {
    if (!data?.deadlineAt || !isInProgress) return;

    function tick() {
      if (!data?.deadlineAt) return;
      const secondsLeft = Math.round((new Date(data.deadlineAt).getTime() - Date.now()) / 1000);
      setRemainingSeconds(secondsLeft);
      if (secondsLeft <= 0) {
        submitNow();
      }
    }

    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [data?.deadlineAt, isInProgress, submitNow]);

  // Warn on tab close/navigation while an attempt is actively in progress.
  useEffect(() => {
    if (!isInProgress) return;
    function handleBeforeUnload(event: BeforeUnloadEvent) {
      event.preventDefault();
    }
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [isInProgress]);

  function flush(questionId: string, next: LocalAnswer) {
    if (debounceTimers.current[questionId]) {
      clearTimeout(debounceTimers.current[questionId]);
      delete debounceTimers.current[questionId];
    }
    upsertAnswer.mutate({ questionId, ...next });
  }

  function scheduleAutosave(questionId: string, next: LocalAnswer) {
    if (debounceTimers.current[questionId]) {
      clearTimeout(debounceTimers.current[questionId]);
    }
    debounceTimers.current[questionId] = setTimeout(() => {
      upsertAnswer.mutate({ questionId, ...next });
    }, AUTOSAVE_DEBOUNCE_MS);
  }

  function setOption(questionId: string, selectedOptionId: string) {
    const next = { selectedOptionId };
    setLocalAnswers((prev) => ({ ...prev, [questionId]: next }));
    // A discrete choice, not free typing - save it immediately rather than debouncing.
    flush(questionId, next);
  }

  function setText(questionId: string, textResponse: string) {
    const next = { textResponse };
    setLocalAnswers((prev) => ({ ...prev, [questionId]: next }));
    scheduleAutosave(questionId, next);
  }

  function handleManualSubmit() {
    const unanswered = (data?.answers ?? []).filter((answer) => {
      const local = localAnswers[answer.questionId];
      return !local?.selectedOptionId && !local?.textResponse?.trim();
    }).length;
    const message =
      unanswered > 0
        ? `You have ${unanswered} unanswered question${unanswered === 1 ? "" : "s"}. Submit anyway? This can't be undone.`
        : "Submit this quiz? This can't be undone.";
    if (window.confirm(message)) {
      submitNow();
    }
  }

  if (attempt.isLoading) {
    return (
      <div className="mx-auto flex w-full max-w-2xl flex-col gap-4 p-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!data) {
    return (
      <div className="mx-auto flex w-full max-w-2xl flex-col gap-4 p-8">
        <p className="text-sm text-muted-foreground">Attempt not found.</p>
      </div>
    );
  }

  if (data.status === "SUBMITTED") {
    return (
      <div className="mx-auto flex w-full max-w-2xl flex-col gap-4 p-8">
        <p className="text-sm text-muted-foreground">
          This attempt has already been submitted{data.autoSubmitted ? " (time ran out)" : ""}.
        </p>
        <Button className="self-start" onClick={() => router.push(`/student/quizzes/${data.quizId}`)}>
          Back to quiz
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 p-8">
      <div className="sticky top-0 z-10 flex flex-col gap-2 bg-background pb-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-medium text-foreground">{data.quizTitle}</h1>
          {remainingSeconds !== null ? (
            <span
              className={
                remainingSeconds <= 60
                  ? "font-mono text-sm font-medium text-destructive"
                  : "font-mono text-sm text-muted-foreground"
              }
            >
              {formatRemaining(remainingSeconds)}
            </span>
          ) : null}
        </div>
        {totalSeconds && remainingSeconds !== null ? (
          <Progress value={Math.max(0, Math.min(100, (remainingSeconds / totalSeconds) * 100))} />
        ) : null}
      </div>

      <div className="flex flex-col gap-4">
        {data.answers.map((answer, index) => {
          const local = localAnswers[answer.questionId] ?? {};
          const isObjective = answer.questionType === "MULTIPLE_CHOICE" || answer.questionType === "TRUE_FALSE";
          return (
            <Card key={answer.questionId}>
              <CardContent className="flex flex-col gap-3">
                <p className="text-sm font-medium text-foreground">
                  {index + 1}. {answer.questionPrompt}{" "}
                  <span className="font-normal text-muted-foreground">({answer.pointsPossible} pts)</span>
                </p>
                {isObjective ? (
                  <RadioGroup
                    value={local.selectedOptionId ?? ""}
                    onValueChange={(value) => setOption(answer.questionId, value)}
                    className="gap-2"
                  >
                    {answer.options.map((option) => (
                      <div key={option.id} className="flex items-center gap-2">
                        <RadioGroupItem value={option.id} id={`${answer.questionId}-${option.id}`} />
                        <label htmlFor={`${answer.questionId}-${option.id}`} className="text-sm text-foreground">
                          {option.text}
                        </label>
                      </div>
                    ))}
                  </RadioGroup>
                ) : (
                  <Textarea
                    rows={4}
                    value={local.textResponse ?? ""}
                    onChange={(event) => setText(answer.questionId, event.target.value)}
                    onBlur={() => flush(answer.questionId, local)}
                  />
                )}
              </CardContent>
            </Card>
          );
        })}
      </div>

      <Button
        type="button"
        disabled={submitAttempt.isPending}
        onClick={handleManualSubmit}
        className="self-start"
      >
        {submitAttempt.isPending ? "Submitting..." : "Submit quiz"}
      </Button>
    </div>
  );
}
