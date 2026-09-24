"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";

import { CodeEditor } from "@/components/assignment/CodeEditor";
import { CodeOutputPanel } from "@/components/assignment/CodeOutputPanel";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { useRunCode } from "@/hooks/useCodeRun";
import { useSubmitAssignment } from "@/hooks/useSubmissions";
import type { CodeRunResult } from "@/types/codeRun";

const submissionSchema = z.object({
  content: z.string().min(1, "Write your code before submitting"),
});

type SubmissionValues = z.infer<typeof submissionSchema>;

interface SubmissionFormProps {
  assignmentId: string;
}

export function SubmissionForm({ assignmentId }: SubmissionFormProps) {
  const submitAssignment = useSubmitAssignment(assignmentId);
  const runCode = useRunCode(assignmentId);
  const [runResult, setRunResult] = useState<CodeRunResult | null>(null);
  const {
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<SubmissionValues>({
    resolver: zodResolver(submissionSchema),
    defaultValues: { content: "" },
  });
  const content = watch("content");

  function onSubmit(values: SubmissionValues) {
    submitAssignment.mutate(values.content);
  }

  function handleRun() {
    setRunResult(null);
    runCode.mutate(content, { onSuccess: setRunResult });
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label>Your code</Label>
        <Controller
          control={control}
          name="content"
          render={({ field }) => <CodeEditor value={field.value} onChange={field.onChange} />}
        />
        {errors.content ? (
          <p className="text-xs text-destructive">{errors.content.message}</p>
        ) : null}
      </div>

      <div className="flex gap-2">
        <Button
          type="button"
          variant="outline"
          disabled={runCode.isPending || !content?.trim()}
          onClick={handleRun}
        >
          {runCode.isPending ? "Running..." : "Run"}
        </Button>
        <Button type="submit" disabled={submitAssignment.isPending}>
          {submitAssignment.isPending ? "Submitting..." : "Submit assignment"}
        </Button>
      </div>

      {runCode.isError ? (
        <p className="text-sm text-destructive">Couldn&apos;t run your code. Try again.</p>
      ) : null}
      {runResult ? <CodeOutputPanel result={runResult} /> : null}

      {submitAssignment.isError ? (
        <p className="text-sm text-destructive">Something went wrong. Try again.</p>
      ) : null}
    </form>
  );
}
