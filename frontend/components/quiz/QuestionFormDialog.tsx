"use client";

import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useFieldArray, useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
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
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAddQuestion, useUpdateQuestion, type QuestionFormInput } from "@/hooks/useQuizzes";
import type { QuestionType, QuizQuestion } from "@/types/quiz";

export const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  MULTIPLE_CHOICE: "Multiple choice",
  TRUE_FALSE: "True / False",
  SHORT_ANSWER: "Short answer",
  ESSAY: "Essay",
};

const questionSchema = z
  .object({
    type: z.enum(["MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER", "ESSAY"]),
    prompt: z.string().min(1, "Prompt is required"),
    pointsPossible: z.coerce.number().min(0),
    referenceAnswer: z.string().optional(),
    options: z.array(z.object({ text: z.string().min(1, "Required") })).optional(),
    correctOptionIndex: z.coerce.number().optional(),
    correctBoolean: z.enum(["true", "false"]).optional(),
  })
  .superRefine((values, ctx) => {
    if (values.type === "MULTIPLE_CHOICE") {
      if (!values.options || values.options.length < 2) {
        ctx.addIssue({
          code: "custom",
          path: ["options"],
          message: "Add at least 2 options",
        });
      }
      if (values.correctOptionIndex === undefined) {
        ctx.addIssue({
          code: "custom",
          path: ["correctOptionIndex"],
          message: "Pick the correct option",
        });
      }
    }
    if (values.type === "TRUE_FALSE" && values.correctBoolean === undefined) {
      ctx.addIssue({
        code: "custom",
        path: ["correctBoolean"],
        message: "Pick the correct answer",
      });
    }
  });

type QuestionValues = z.infer<typeof questionSchema>;

function toValues(question?: QuizQuestion): QuestionValues {
  if (!question) {
    return {
      type: "MULTIPLE_CHOICE",
      prompt: "",
      pointsPossible: 1,
      options: [{ text: "" }, { text: "" }],
      correctOptionIndex: 0,
    };
  }
  const correctIndex = question.options.findIndex((option) => option.correct);
  return {
    type: question.type,
    prompt: question.prompt,
    pointsPossible: question.pointsPossible,
    referenceAnswer: question.referenceAnswer ?? "",
    options:
      question.type === "MULTIPLE_CHOICE"
        ? question.options.map((option) => ({ text: option.text }))
        : [{ text: "" }, { text: "" }],
    correctOptionIndex: question.type === "MULTIPLE_CHOICE" && correctIndex >= 0 ? correctIndex : 0,
    correctBoolean:
      question.type === "TRUE_FALSE"
        ? question.options.find((option) => option.text === "True")?.correct
          ? "true"
          : "false"
        : undefined,
  };
}

function toInput(values: QuestionValues): QuestionFormInput {
  if (values.type === "MULTIPLE_CHOICE") {
    return {
      type: values.type,
      prompt: values.prompt,
      pointsPossible: values.pointsPossible,
      options: (values.options ?? []).map((option, index) => ({
        text: option.text,
        correct: index === values.correctOptionIndex,
      })),
    };
  }
  if (values.type === "TRUE_FALSE") {
    return {
      type: values.type,
      prompt: values.prompt,
      pointsPossible: values.pointsPossible,
      correctBoolean: values.correctBoolean === "true",
    };
  }
  return {
    type: values.type,
    prompt: values.prompt,
    pointsPossible: values.pointsPossible,
    referenceAnswer: values.referenceAnswer || undefined,
  };
}

interface QuestionFormDialogProps {
  quizId: string;
  /** Omit to add a new question; pass an existing one to edit it in place. */
  question?: QuizQuestion;
  trigger: React.ReactNode;
}

export function QuestionFormDialog({ quizId, question, trigger }: QuestionFormDialogProps) {
  const [open, setOpen] = useState(false);
  const addQuestion = useAddQuestion(quizId);
  const updateQuestion = useUpdateQuestion(quizId);
  const isPending = addQuestion.isPending || updateQuestion.isPending;

  const {
    register,
    control,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<QuestionValues>({
    resolver: zodResolver(questionSchema),
    defaultValues: toValues(question),
  });
  const { fields, append, remove } = useFieldArray({ control, name: "options" });
  const type = watch("type");

  // Re-seed the form whenever a different question is opened for editing (the dialog instance is
  // reused across rows rather than remounted per-question).
  useEffect(() => {
    if (open) {
      reset(toValues(question));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function onSubmit(values: QuestionValues) {
    const input = toInput(values);
    const onSuccess = () => {
      reset();
      setOpen(false);
    };
    if (question) {
      updateQuestion.mutate({ questionId: question.id, input }, { onSuccess });
    } else {
      addQuestion.mutate(input, { onSuccess });
    }
  }

  return (
    <Dialog open={open} onOpenChange={(next) => { setOpen(next); if (!next) reset(); }}>
      <DialogTrigger render={trigger as React.ReactElement} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{question ? "Edit question" : "New question"}</DialogTitle>
        </DialogHeader>
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="flex max-h-[70vh] flex-col gap-4 overflow-y-auto"
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="question-type">Type</Label>
            <Controller
              control={control}
              name="type"
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange} disabled={!!question}>
                  <SelectTrigger id="question-type" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {(Object.keys(QUESTION_TYPE_LABELS) as QuestionType[]).map((value) => (
                      <SelectItem key={value} value={value}>
                        {QUESTION_TYPE_LABELS[value]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {question ? (
              <p className="text-xs text-muted-foreground">
                Type can&apos;t change after a question is created — delete and re-add instead.
              </p>
            ) : null}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="question-prompt">Prompt</Label>
            <Textarea id="question-prompt" rows={3} {...register("prompt")} />
            {errors.prompt ? <p className="text-xs text-destructive">{errors.prompt.message}</p> : null}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="question-points">Points possible</Label>
            <Input
              id="question-points"
              type="number"
              min={0}
              step="0.5"
              className="w-24"
              {...register("pointsPossible")}
            />
          </div>

          {type === "MULTIPLE_CHOICE" ? (
            <div className="flex flex-col gap-2">
              <Label>Options — select the correct one</Label>
              <Controller
                control={control}
                name="correctOptionIndex"
                render={({ field }) => (
                  <RadioGroup
                    value={String(field.value ?? "")}
                    onValueChange={(value) => field.onChange(Number(value))}
                    className="gap-2"
                  >
                    {fields.map((option, index) => (
                      <div key={option.id} className="flex items-center gap-2">
                        <RadioGroupItem value={String(index)} id={`option-correct-${index}`} />
                        <Input
                          {...register(`options.${index}.text` as const)}
                          placeholder={`Option ${index + 1}`}
                          className="flex-1"
                        />
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          disabled={fields.length <= 2}
                          onClick={() => remove(index)}
                        >
                          Remove
                        </Button>
                      </div>
                    ))}
                  </RadioGroup>
                )}
              />
              {errors.options ? (
                <p className="text-xs text-destructive">{errors.options.message}</p>
              ) : null}
              {errors.correctOptionIndex ? (
                <p className="text-xs text-destructive">{errors.correctOptionIndex.message}</p>
              ) : null}
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="self-start"
                onClick={() => append({ text: "" })}
              >
                Add option
              </Button>
            </div>
          ) : null}

          {type === "TRUE_FALSE" ? (
            <div className="flex flex-col gap-2">
              <Label>Correct answer</Label>
              <Controller
                control={control}
                name="correctBoolean"
                render={({ field }) => (
                  <RadioGroup value={field.value} onValueChange={field.onChange} className="gap-2">
                    <div className="flex items-center gap-2">
                      <RadioGroupItem value="true" id="correct-true" />
                      <Label htmlFor="correct-true" className="font-normal">True</Label>
                    </div>
                    <div className="flex items-center gap-2">
                      <RadioGroupItem value="false" id="correct-false" />
                      <Label htmlFor="correct-false" className="font-normal">False</Label>
                    </div>
                  </RadioGroup>
                )}
              />
              {errors.correctBoolean ? (
                <p className="text-xs text-destructive">{errors.correctBoolean.message}</p>
              ) : null}
            </div>
          ) : null}

          {type === "SHORT_ANSWER" || type === "ESSAY" ? (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="question-reference">Reference answer / grading guide (optional)</Label>
              <Textarea id="question-reference" rows={3} {...register("referenceAnswer")} />
              <p className="text-xs text-muted-foreground">
                Not shown to students. Used as your own grading notes, and as context if you ask
                Sage to draft a suggested score.
              </p>
            </div>
          ) : null}

          <DialogFooter>
            <Button type="submit" disabled={isPending}>
              {isPending ? "Saving..." : question ? "Save changes" : "Add question"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
