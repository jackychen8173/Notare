"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useSubmitAssignment } from "@/hooks/useSubmissions";

const submissionSchema = z.object({
  content: z.string().min(1, "Paste or write your code before submitting"),
});

type SubmissionValues = z.infer<typeof submissionSchema>;

interface SubmissionFormProps {
  assignmentId: string;
}

export function SubmissionForm({ assignmentId }: SubmissionFormProps) {
  const submitAssignment = useSubmitAssignment(assignmentId);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SubmissionValues>({ resolver: zodResolver(submissionSchema) });

  function onSubmit(values: SubmissionValues) {
    submitAssignment.mutate(values.content);
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="content">Your code</Label>
        <Textarea
          id="content"
          rows={12}
          className="font-mono text-xs"
          placeholder="Paste your Java code here..."
          {...register("content")}
        />
        {errors.content ? (
          <p className="text-xs text-destructive">{errors.content.message}</p>
        ) : null}
      </div>
      <Button type="submit" disabled={submitAssignment.isPending} className="self-start">
        {submitAssignment.isPending ? "Submitting..." : "Submit assignment"}
      </Button>
      {submitAssignment.isError ? (
        <p className="text-sm text-destructive">Something went wrong. Try again.</p>
      ) : null}
    </form>
  );
}
