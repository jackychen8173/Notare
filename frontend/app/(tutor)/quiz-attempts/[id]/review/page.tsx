"use client";

import { use } from "react";

import { QuizAttemptReview } from "@/components/quiz/QuizAttemptReview";
import { Skeleton } from "@/components/ui/skeleton";
import { useQuizAttempt } from "@/hooks/useQuizAttempts";

export default function QuizAttemptReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const attempt = useQuizAttempt(id);

  if (attempt.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!attempt.data) {
    return <p className="text-sm text-muted-foreground">Attempt not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-medium text-foreground">Review quiz attempt</h1>
      <QuizAttemptReview attempt={attempt.data} />
    </div>
  );
}
