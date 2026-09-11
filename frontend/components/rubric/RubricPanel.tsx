"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useFieldArray, useForm } from "react-hook-form";
import { z } from "zod";

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
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useCreateRubric, useDeleteRubric, useRubric } from "@/hooks/useRubrics";

const rubricSchema = z.object({
  title: z.string().min(1, "Title is required"),
  criteria: z
    .array(
      z.object({
        name: z.string().min(1, "Name is required"),
        description: z.string().optional(),
        pointsPossible: z.coerce.number().min(0),
      }),
    )
    .min(1, "Add at least one criterion"),
});
type RubricValues = z.infer<typeof rubricSchema>;

function NewRubricDialog({ assignmentId }: { assignmentId: string }) {
  const [open, setOpen] = useState(false);
  const createRubric = useCreateRubric(assignmentId);
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<RubricValues>({
    resolver: zodResolver(rubricSchema),
    defaultValues: { title: "", criteria: [{ name: "", description: "", pointsPossible: 10 }] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "criteria" });

  function onSubmit(values: RubricValues) {
    createRubric.mutate(values, {
      onSuccess: () => {
        reset();
        setOpen(false);
      },
    });
  }

  return (
    <Dialog open={open} onOpenChange={(next) => { setOpen(next); if (!next) reset(); }}>
      <DialogTrigger render={<Button variant="outline">Add rubric</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New rubric</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex max-h-[70vh] flex-col gap-4 overflow-y-auto">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="rubric-title">Title</Label>
            <Input id="rubric-title" {...register("title")} />
            {errors.title ? <p className="text-xs text-destructive">{errors.title.message}</p> : null}
          </div>

          <div className="flex flex-col gap-3">
            <Label>Criteria</Label>
            {fields.map((field, index) => (
              <Card key={field.id}>
                <CardContent className="flex flex-col gap-2">
                  <div className="flex items-end gap-2">
                    <div className="flex flex-1 flex-col gap-1.5">
                      <Label htmlFor={`criteria.${index}.name`}>Name</Label>
                      <Input id={`criteria.${index}.name`} {...register(`criteria.${index}.name` as const)} />
                    </div>
                    <div className="flex w-24 flex-col gap-1.5">
                      <Label htmlFor={`criteria.${index}.pointsPossible`}>Points</Label>
                      <Input
                        id={`criteria.${index}.pointsPossible`}
                        type="number"
                        min={0}
                        step="0.5"
                        {...register(`criteria.${index}.pointsPossible` as const)}
                      />
                    </div>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={fields.length <= 1}
                      onClick={() => remove(index)}
                    >
                      Remove
                    </Button>
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label htmlFor={`criteria.${index}.description`}>Description (optional)</Label>
                    <Textarea
                      id={`criteria.${index}.description`}
                      rows={2}
                      {...register(`criteria.${index}.description` as const)}
                    />
                  </div>
                  {errors.criteria?.[index]?.name ? (
                    <p className="text-xs text-destructive">{errors.criteria[index]?.name?.message}</p>
                  ) : null}
                </CardContent>
              </Card>
            ))}
            {errors.criteria?.message ? (
              <p className="text-xs text-destructive">{errors.criteria.message}</p>
            ) : null}
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="self-start"
              onClick={() => append({ name: "", description: "", pointsPossible: 10 })}
            >
              Add criterion
            </Button>
          </div>

          <DialogFooter>
            <Button type="submit" disabled={createRubric.isPending}>
              {createRubric.isPending ? "Creating..." : "Create rubric"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function RubricPanel({ assignmentId }: { assignmentId: string }) {
  const rubric = useRubric(assignmentId);
  const deleteRubric = useDeleteRubric(assignmentId);

  if (rubric.isLoading) {
    return <Skeleton className="h-16 w-full" />;
  }

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-medium text-foreground">Rubric</h2>
        {!rubric.data ? <NewRubricDialog assignmentId={assignmentId} /> : null}
      </div>
      {rubric.data ? (
        <Card>
          <CardContent className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <p className="font-medium text-foreground">{rubric.data.title}</p>
              <Button variant="outline" size="sm" disabled={deleteRubric.isPending} onClick={() => deleteRubric.mutate()}>
                {deleteRubric.isPending ? "Deleting..." : "Delete rubric"}
              </Button>
            </div>
            <div className="flex flex-col gap-2">
              {rubric.data.criteria.map((criterion) => (
                <div key={criterion.id} className="flex items-start justify-between gap-4 border-t-hairline border-border pt-2 first:border-t-0 first:pt-0">
                  <div>
                    <p className="text-sm font-medium text-foreground">{criterion.name}</p>
                    {criterion.description ? (
                      <p className="text-xs text-muted-foreground">{criterion.description}</p>
                    ) : null}
                  </div>
                  <p className="whitespace-nowrap text-sm text-muted-foreground">{criterion.pointsPossible} pts</p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent>
            <p className="text-sm text-muted-foreground">No rubric yet.</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
