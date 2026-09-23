"use client";

import { use, useState } from "react";
import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";

import { QUESTION_TYPE_LABELS, QuestionFormDialog } from "@/components/quiz/QuestionFormDialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useQuizAttempts } from "@/hooks/useQuizAttempts";
import {
  useDeleteQuestion,
  usePublishQuiz,
  useQuiz,
  useUnpublishQuiz,
  useUpdateQuiz,
  type QuizFormInput,
} from "@/hooks/useQuizzes";
import { useGradeCategories } from "@/hooks/useGradeCategories";
import { useTopics } from "@/hooks/useTopics";
import { cn } from "@/lib/utils";
import type { AttemptStatus } from "@/types/quizAttempt";
import type { Quiz, QuizQuestion } from "@/types/quiz";

const NO_TOPIC = "__none__";

const attemptStatusVariant: Record<AttemptStatus, "default" | "secondary"> = {
  IN_PROGRESS: "secondary",
  SUBMITTED: "default",
};

const editQuizSchema = z.object({
  title: z.string().min(1, "Title is required"),
  description: z.string().optional(),
  timeLimitMinutes: z.string().optional(),
  topicId: z.string().optional(),
  gradeCategoryId: z.string().optional(),
});

type EditQuizValues = z.infer<typeof editQuizSchema>;

function EditQuizDialog({ quiz }: { quiz: Quiz }) {
  const [open, setOpen] = useState(false);
  const updateQuiz = useUpdateQuiz(quiz.id);
  const topics = useTopics(quiz.courseId);
  const gradeCategories = useGradeCategories(quiz.courseId);
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditQuizValues>({
    resolver: zodResolver(editQuizSchema),
    values: {
      title: quiz.title,
      description: quiz.description ?? "",
      timeLimitMinutes: quiz.timeLimitMinutes ? String(quiz.timeLimitMinutes) : "",
      topicId: quiz.topicId ?? NO_TOPIC,
      gradeCategoryId: quiz.gradeCategoryId ?? NO_TOPIC,
    },
  });

  function onSubmit(values: EditQuizValues) {
    const input: QuizFormInput = {
      title: values.title,
      description: values.description || undefined,
      timeLimitMinutes: values.timeLimitMinutes ? Number(values.timeLimitMinutes) : undefined,
      topicId: values.topicId === NO_TOPIC ? undefined : values.topicId,
      gradeCategoryId: values.gradeCategoryId === NO_TOPIC ? undefined : values.gradeCategoryId,
    };
    updateQuiz.mutate(input, { onSuccess: () => setOpen(false) });
  }

  return (
    <Dialog open={open} onOpenChange={(next) => { setOpen(next); if (!next) reset(); }}>
      <DialogTrigger render={<Button variant="outline">Edit quiz</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit quiz</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="edit-quiz-title">Title</Label>
            <Input id="edit-quiz-title" {...register("title")} />
            {errors.title ? <p className="text-xs text-destructive">{errors.title.message}</p> : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="edit-quiz-description">Description (optional)</Label>
            <Textarea id="edit-quiz-description" rows={3} {...register("description")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="edit-quiz-time-limit">Time limit, minutes (optional)</Label>
            <Input id="edit-quiz-time-limit" type="number" min={1} {...register("timeLimitMinutes")} />
          </div>
          {topics.data && topics.data.length > 0 ? (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-quiz-topicId">Topic (optional)</Label>
              <Controller
                control={control}
                name="topicId"
                render={({ field }) => (
                  <Select value={field.value ?? NO_TOPIC} onValueChange={field.onChange}>
                    <SelectTrigger id="edit-quiz-topicId" className="w-full">
                      <SelectValue placeholder="No topic" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NO_TOPIC}>No topic</SelectItem>
                      {topics.data?.map((topic) => (
                        <SelectItem key={topic.id} value={topic.id}>
                          {topic.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
          ) : null}
          {gradeCategories.data && gradeCategories.data.length > 0 ? (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-quiz-gradeCategoryId">Grade category (optional)</Label>
              <Controller
                control={control}
                name="gradeCategoryId"
                render={({ field }) => (
                  <Select value={field.value ?? NO_TOPIC} onValueChange={field.onChange}>
                    <SelectTrigger id="edit-quiz-gradeCategoryId" className="w-full">
                      <SelectValue placeholder="No category" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NO_TOPIC}>No category</SelectItem>
                      {gradeCategories.data?.map((category) => (
                        <SelectItem key={category.id} value={category.id}>
                          {category.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
          ) : null}
          <DialogFooter>
            <Button type="submit" disabled={updateQuiz.isPending}>
              {updateQuiz.isPending ? "Saving..." : "Save changes"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function PublishControl({ quiz }: { quiz: Quiz }) {
  const publishQuiz = usePublishQuiz(quiz.id);
  const unpublishQuiz = useUnpublishQuiz(quiz.id);

  if (quiz.publishedAt) {
    return (
      <Button variant="outline" disabled={unpublishQuiz.isPending} onClick={() => unpublishQuiz.mutate()}>
        {unpublishQuiz.isPending ? "Unpublishing..." : "Unpublish"}
      </Button>
    );
  }

  return (
    <Button disabled={publishQuiz.isPending || quiz.questions.length === 0} onClick={() => publishQuiz.mutate()}>
      {publishQuiz.isPending ? "Publishing..." : "Publish"}
    </Button>
  );
}

function QuestionRow({ quizId, question, index }: { quizId: string; question: QuizQuestion; index: number }) {
  const deleteQuestion = useDeleteQuestion(quizId);
  const showsOptions = question.type === "MULTIPLE_CHOICE" || question.type === "TRUE_FALSE";

  return (
    <Card>
      <CardContent className="flex flex-col gap-2">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-medium text-muted-foreground">
              {index + 1}. {QUESTION_TYPE_LABELS[question.type]} · {question.pointsPossible} pts
            </p>
            <p className="text-sm text-foreground">{question.prompt}</p>
          </div>
          <div className="flex shrink-0 gap-2">
            <QuestionFormDialog
              quizId={quizId}
              question={question}
              trigger={<Button variant="outline" size="sm">Edit</Button>}
            />
            <Button
              variant="outline"
              size="sm"
              disabled={deleteQuestion.isPending}
              onClick={() => deleteQuestion.mutate(question.id)}
            >
              {deleteQuestion.isPending ? "Removing..." : "Remove"}
            </Button>
          </div>
        </div>
        {showsOptions ? (
          <ul className="flex flex-col gap-0.5">
            {question.options.map((option) => (
              <li
                key={option.id}
                className={cn(
                  "text-xs",
                  option.correct ? "font-medium text-foreground" : "text-muted-foreground",
                )}
              >
                {option.correct ? "✓ " : "— "}
                {option.text}
              </li>
            ))}
          </ul>
        ) : null}
      </CardContent>
    </Card>
  );
}

function AttemptsList({ quizId }: { quizId: string }) {
  const attempts = useQuizAttempts(quizId);

  if (attempts.isLoading) {
    return <Skeleton className="h-16 w-full" />;
  }

  if (!attempts.data || attempts.data.length === 0) {
    return (
      <Card>
        <CardContent>
          <p className="text-sm text-muted-foreground">No attempts yet.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      {attempts.data.map((attempt) => (
        <Link key={attempt.id} href={`/quiz-attempts/${attempt.id}/review`}>
          <Card className="transition-colors hover:bg-muted/40">
            <CardContent className="flex items-center justify-between gap-4">
              <div>
                <p className="font-medium text-foreground">{attempt.studentName}</p>
                <p className="text-xs text-muted-foreground">
                  {attempt.submittedAt
                    ? `Submitted ${new Date(attempt.submittedAt).toLocaleString()}${attempt.autoSubmitted ? " (auto-submitted)" : ""}`
                    : "In progress"}
                </p>
              </div>
              <div className="flex items-center gap-3">
                {attempt.releasedAt ? (
                  <span className="text-sm text-muted-foreground">
                    {attempt.totalPointsAwarded ?? 0} / {attempt.totalPointsPossible ?? 0}
                  </span>
                ) : null}
                <Badge variant={attemptStatusVariant[attempt.status]}>
                  {attempt.releasedAt ? "Released" : attempt.status}
                </Badge>
              </div>
            </CardContent>
          </Card>
        </Link>
      ))}
    </div>
  );
}

export default function QuizDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const quiz = useQuiz(id);

  if (quiz.isLoading) {
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

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-medium text-foreground">{quiz.data.title}</h1>
            <Badge variant={quiz.data.publishedAt ? "default" : "secondary"}>
              {quiz.data.publishedAt ? "Published" : "Draft"}
            </Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            {quiz.data.courseName}
            {quiz.data.timeLimitMinutes ? ` · ${quiz.data.timeLimitMinutes} min, hard cutoff` : " · Untimed"}
            {" · "}
            {quiz.data.totalPointsPossible} pts total
          </p>
          {quiz.data.description ? (
            <p className="mt-2 text-sm text-muted-foreground">{quiz.data.description}</p>
          ) : null}
          <div className="mt-3 flex gap-2">
            <EditQuizDialog quiz={quiz.data} />
            <PublishControl quiz={quiz.data} />
          </div>
        </div>
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-medium text-foreground">Questions</h2>
          <QuestionFormDialog
            quizId={id}
            trigger={<Button variant="outline">Add question</Button>}
          />
        </div>
        {quiz.data.questions.length > 0 ? (
          <div className="flex flex-col gap-3">
            {quiz.data.questions.map((question, index) => (
              <QuestionRow key={question.id} quizId={id} question={question} index={index} />
            ))}
          </div>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                No questions yet. A quiz needs at least one to be published.
              </p>
            </CardContent>
          </Card>
        )}
      </div>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Attempts</h2>
        <AttemptsList quizId={id} />
      </div>
    </div>
  );
}
