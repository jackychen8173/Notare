"use client";

import { useState, type ReactElement } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm, useWatch } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useCreateAssignment, useUpdateAssignment } from "@/hooks/useAssignments";
import { useGradeCategories } from "@/hooks/useGradeCategories";
import { useTopics } from "@/hooks/useTopics";
import { errorMessage } from "@/lib/api";
import { browserTimeZone, wallClockParts, zoneLabel } from "@/lib/dates";
import type { Assignment } from "@/types/assignment";

const NONE = "__none__";

/** accept: late work any time. penalty: late work with points off per day. lock: nothing after the due time. */
type LatePolicy = "accept" | "penalty" | "lock";

const optionalWholeNumber = (min: number, max: number, message: string) =>
  z
    .string()
    .trim()
    .refine((v) => v === "" || (/^\d+$/.test(v) && Number(v) >= min && Number(v) <= max), message);

const schema = z
  .object({
    title: z.string().min(1, "Title is required"),
    description: z.string().optional(),
    dueDate: z.string().min(1, "Due date is required"),
    dueTime: z.string().min(1, "Due time is required"),
    points: optionalWholeNumber(1, 10000, "Points must be a whole number from 1 to 10,000"),
    latePolicy: z.enum(["accept", "penalty", "lock"]),
    penalty: z.string(),
    stopLate: z.boolean(),
    closesDate: z.string(),
    closesTime: z.string(),
    scheduled: z.boolean(),
    opensDate: z.string(),
    opensTime: z.string(),
    topicId: z.string().optional(),
    gradeCategoryId: z.string().optional(),
    allowResubmission: z.boolean().optional(),
  })
  .superRefine((v, ctx) => {
    const due = `${v.dueDate}T${v.dueTime}`;
    if (v.latePolicy === "penalty" && !(/^\d+$/.test(v.penalty.trim()) && Number(v.penalty) >= 1 && Number(v.penalty) <= 100)) {
      ctx.addIssue({ code: "custom", path: ["penalty"], message: "Enter a percent from 1 to 100" });
    }
    if (v.latePolicy !== "lock" && v.stopLate) {
      if (!v.closesDate || !v.closesTime) {
        ctx.addIssue({ code: "custom", path: ["closesDate"], message: "Pick when late work stops" });
      } else if (`${v.closesDate}T${v.closesTime}` < due) {
        ctx.addIssue({ code: "custom", path: ["closesDate"], message: "This has to be after the due time" });
      }
    }
    if (v.scheduled) {
      if (!v.opensDate || !v.opensTime) {
        ctx.addIssue({ code: "custom", path: ["opensDate"], message: "Pick when students can see it" });
      } else if (`${v.opensDate}T${v.opensTime}` >= due) {
        ctx.addIssue({ code: "custom", path: ["opensDate"], message: "This has to be before the due time" });
      }
    }
  });

type Values = z.infer<typeof schema>;

function defaults(assignment?: Assignment): Values {
  if (!assignment) {
    return {
      title: "",
      description: "",
      dueDate: "",
      dueTime: "23:59",
      points: "100",
      latePolicy: "accept",
      penalty: "10",
      stopLate: false,
      closesDate: "",
      closesTime: "23:59",
      scheduled: false,
      opensDate: "",
      opensTime: "08:00",
      topicId: undefined,
      gradeCategoryId: undefined,
      allowResubmission: false,
    };
  }
  const due = wallClockParts(assignment.dueAt);
  const closes = assignment.closesAt ? wallClockParts(assignment.closesAt) : null;
  const opens = assignment.opensAt ? wallClockParts(assignment.opensAt) : null;
  const locked = assignment.closesAt != null && assignment.closesAt === assignment.dueAt;
  return {
    title: assignment.title,
    description: assignment.description ?? "",
    dueDate: due.date,
    dueTime: due.time,
    points: assignment.pointsPossible != null ? String(assignment.pointsPossible) : "",
    latePolicy: locked ? "lock" : assignment.latePenaltyPercent > 0 ? "penalty" : "accept",
    penalty: assignment.latePenaltyPercent > 0 ? String(assignment.latePenaltyPercent) : "10",
    stopLate: closes != null && !locked,
    closesDate: closes?.date ?? "",
    closesTime: closes?.time ?? "23:59",
    scheduled: opens != null,
    opensDate: opens?.date ?? "",
    opensTime: opens?.time ?? "08:00",
    topicId: assignment.topicId ?? undefined,
    gradeCategoryId: assignment.gradeCategoryId ?? undefined,
    allowResubmission: assignment.allowResubmission,
  };
}

/** Wall-clock times in the course's zone, as the API takes them ("2026-11-20T23:59"). */
function toTiming(v: Values) {
  const dueAt = `${v.dueDate}T${v.dueTime}`;
  return {
    dueAt,
    opensAt: v.scheduled ? `${v.opensDate}T${v.opensTime}` : null,
    closesAt: v.latePolicy === "lock" ? dueAt : v.stopLate ? `${v.closesDate}T${v.closesTime}` : null,
    pointsPossible: v.points.trim() === "" ? null : Number(v.points),
    latePenaltyPercent: v.latePolicy === "penalty" ? Number(v.penalty) : 0,
  };
}

function FieldError({ message }: { message?: string }) {
  return message ? <p className="text-xs text-destructive">{message}</p> : null;
}

/**
 * Create or edit an assignment: title, due time, points and late rules. Creating also sets the
 * unit and resubmission; editing leaves those to the settings card on the assignment page.
 */
export function AssignmentFormDialog({
  courseId,
  timeZone,
  assignment,
  trigger,
}: {
  courseId: string;
  timeZone: string;
  assignment?: Assignment;
  trigger: ReactElement;
}) {
  const editing = assignment != null;
  const [open, setOpen] = useState(false);
  const create = useCreateAssignment(courseId);
  const update = useUpdateAssignment(assignment?.id ?? "");
  const mutation = editing ? update : create;
  const topics = useTopics(courseId);
  const gradeCategories = useGradeCategories(courseId);
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: defaults(assignment) });
  const latePolicy = useWatch({ control, name: "latePolicy" });
  const stopLate = useWatch({ control, name: "stopLate" });
  const scheduled = useWatch({ control, name: "scheduled" });

  function onSubmit(values: Values) {
    const gradeCategoryId = values.gradeCategoryId && values.gradeCategoryId !== NONE ? values.gradeCategoryId : null;
    const onSuccess = () => setOpen(false);
    if (editing) {
      update.mutate(
        { title: values.title, description: values.description || null, gradeCategoryId, ...toTiming(values) },
        { onSuccess },
      );
    } else {
      create.mutate(
        {
          title: values.title,
          description: values.description || undefined,
          topicId: values.topicId && values.topicId !== NONE ? values.topicId : undefined,
          gradeCategoryId: gradeCategoryId ?? undefined,
          allowResubmission: values.allowResubmission,
          ...toTiming(values),
        },
        { onSuccess },
      );
    }
  }

  const zoneHint =
    timeZone !== browserTimeZone() ? `Times are in the course's time zone (${zoneLabel(timeZone)}).` : null;

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (nextOpen) reset(defaults(assignment));
      }}
    >
      <DialogTrigger render={trigger} />
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{editing ? "Edit assignment" : "New assignment"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="assignment-title">Title</Label>
            <Input id="assignment-title" {...register("title")} />
            <FieldError message={errors.title?.message} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="assignment-description">Description (optional)</Label>
            <Textarea id="assignment-description" rows={3} {...register("description")} />
          </div>

          <fieldset className="flex flex-col gap-1.5">
            <legend className="mb-1.5 text-sm font-medium text-foreground">Due</legend>
            <div className="flex gap-2">
              <Input aria-label="Due date" type="date" className="flex-1" {...register("dueDate")} />
              <Input aria-label="Due time" type="time" className="w-32" {...register("dueTime")} />
            </div>
            <FieldError message={errors.dueDate?.message ?? errors.dueTime?.message} />
            {zoneHint ? <p className="text-xs text-muted-foreground">{zoneHint}</p> : null}
          </fieldset>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="assignment-points">Points</Label>
            <Input
              id="assignment-points"
              inputMode="numeric"
              className="w-32"
              placeholder="Not scored"
              {...register("points")}
            />
            <p className="text-xs text-muted-foreground">Leave it empty for work you don&apos;t score in points.</p>
            <FieldError message={errors.points?.message} />
          </div>

          <fieldset className="flex flex-col gap-2">
            <legend className="mb-1.5 text-sm font-medium text-foreground">Late work</legend>
            <Controller
              control={control}
              name="latePolicy"
              render={({ field }) => (
                <div className="flex flex-col gap-1.5" role="radiogroup">
                  {(
                    [
                      ["accept", "Accept it, marked late"],
                      ["penalty", "Accept it with points off"],
                      ["lock", "Don't accept it: close at the due time"],
                    ] as [LatePolicy, string][]
                  ).map(([policy, label]) => (
                    <label key={policy} className="flex items-center gap-2 text-sm text-foreground">
                      <input
                        type="radio"
                        className="accent-primary"
                        checked={field.value === policy}
                        onChange={() => field.onChange(policy)}
                      />
                      {label}
                    </label>
                  ))}
                </div>
              )}
            />
            {latePolicy === "penalty" ? (
              <div className="flex items-center gap-2 pl-6 text-sm text-muted-foreground">
                <Input aria-label="Percent off per day" inputMode="numeric" className="w-20" {...register("penalty")} />
                % off for each day late (any part of a day counts)
              </div>
            ) : null}
            <FieldError message={errors.penalty?.message} />
            {latePolicy !== "lock" ? (
              <div className="flex flex-col gap-1.5 pl-6">
                <label className="flex items-center gap-2 text-sm text-foreground">
                  <input type="checkbox" className="accent-primary" {...register("stopLate")} />
                  Stop accepting late work after a date
                </label>
                {stopLate ? (
                  <div className="flex gap-2">
                    <Input aria-label="Closes on" type="date" className="flex-1" {...register("closesDate")} />
                    <Input aria-label="Closes at" type="time" className="w-32" {...register("closesTime")} />
                  </div>
                ) : null}
                <FieldError message={errors.closesDate?.message} />
              </div>
            ) : null}
          </fieldset>

          <div className="flex flex-col gap-1.5">
            <label className="flex items-start gap-2 text-sm text-foreground">
              <input type="checkbox" className="mt-1 accent-primary" {...register("scheduled")} />
              <span>
                Schedule it
                <span className="block text-xs text-muted-foreground">
                  Students don&apos;t see the assignment until this time.
                </span>
              </span>
            </label>
            {scheduled ? (
              <div className="flex gap-2 pl-6">
                <Input aria-label="Opens on" type="date" className="flex-1" {...register("opensDate")} />
                <Input aria-label="Opens at" type="time" className="w-32" {...register("opensTime")} />
              </div>
            ) : null}
            <FieldError message={errors.opensDate?.message} />
          </div>

          {!editing && topics.data && topics.data.length > 0 ? (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="assignment-topic">Unit (optional)</Label>
              <Controller
                control={control}
                name="topicId"
                render={({ field }) => (
                  <Select value={field.value ?? NONE} onValueChange={field.onChange}>
                    <SelectTrigger id="assignment-topic" className="w-full">
                      <SelectValue placeholder="No unit">
                        {(value: string) =>
                          value === NONE ? "No unit" : (topics.data?.find((t) => t.id === value)?.name ?? "No unit")
                        }
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NONE}>No unit</SelectItem>
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
              <Label htmlFor="assignment-category">Grade category (optional)</Label>
              <Controller
                control={control}
                name="gradeCategoryId"
                render={({ field }) => (
                  <Select value={field.value ?? NONE} onValueChange={field.onChange}>
                    <SelectTrigger id="assignment-category" className="w-full">
                      <SelectValue placeholder="No category">
                        {(value: string) =>
                          value === NONE
                            ? "No category"
                            : (gradeCategories.data?.find((c) => c.id === value)?.name ?? "No category")
                        }
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NONE}>No category</SelectItem>
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
          {!editing ? (
            <label className="flex items-start gap-2 text-sm text-foreground">
              <input type="checkbox" className="mt-1 accent-primary" {...register("allowResubmission")} />
              <span>
                Allow resubmission
                <span className="block text-xs text-muted-foreground">
                  After you release feedback, students can fix their code and submit a new version.
                </span>
              </span>
            </label>
          ) : null}

          {mutation.isError ? <p className="text-xs text-destructive">{errorMessage(mutation.error)}</p> : null}
          <DialogFooter>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Saving..." : editing ? "Save changes" : "Create assignment"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
