"use client";

import { use } from "react";

import { QuizTakeForm } from "@/components/quiz/QuizTakeForm";

export default function QuizTakePage({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = use(params);
  return <QuizTakeForm attemptId={attemptId} />;
}
