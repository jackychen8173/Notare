"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { IconBook2 } from "@tabler/icons-react";
import { z } from "zod";

import { CourseCard } from "@/components/course/CourseCard";
import { EmptyState } from "@/components/layout/EmptyState";
import { PageHeader } from "@/components/layout/PageHeader";
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
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useCourses, useCreateCourse } from "@/hooks/useCourses";

const createCourseSchema = z.object({
  name: z.string().min(1, "Name is required"),
  subject: z.string().min(1, "Subject is required"),
  description: z.string().optional(),
});

type CreateCourseValues = z.infer<typeof createCourseSchema>;

function NewCourseDialog() {
  const [open, setOpen] = useState(false);
  const createCourse = useCreateCourse();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateCourseValues>({ resolver: zodResolver(createCourseSchema) });

  function onSubmit(values: CreateCourseValues) {
    createCourse.mutate(values, {
      onSuccess: () => {
        reset();
        setOpen(false);
      },
    });
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (!nextOpen) reset();
      }}
    >
      <DialogTrigger render={<Button>New course</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New course</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="name">Name</Label>
            <Input id="name" {...register("name")} />
            {errors.name ? <p className="text-xs text-destructive">{errors.name.message}</p> : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="subject">Subject</Label>
            <Input id="subject" {...register("subject")} />
            {errors.subject ? (
              <p className="text-xs text-destructive">{errors.subject.message}</p>
            ) : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="description">Description (optional)</Label>
            <Textarea id="description" rows={3} {...register("description")} />
          </div>
          <DialogFooter>
            <Button type="submit" disabled={createCourse.isPending}>
              {createCourse.isPending ? "Creating..." : "Create course"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function CoursesPage() {
  const [archived, setArchived] = useState(false);
  const courses = useCourses(archived);

  return (
    <>
      <PageHeader
        title="Courses"
        description="Courses you teach and their enrolled students."
        actions={<NewCourseDialog />}
      />

      <div className="mb-6 inline-flex rounded-lg bg-muted p-1">
        {[false, true].map((value) => (
          <button
            key={String(value)}
            type="button"
            onClick={() => setArchived(value)}
            className={
              archived === value
                ? "rounded-md bg-card px-3 py-1 text-sm font-medium text-foreground shadow-xs"
                : "rounded-md px-3 py-1 text-sm font-medium text-muted-foreground hover:text-foreground"
            }
          >
            {value ? "Archived" : "Active"}
          </button>
        ))}
      </div>

      {courses.isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-40 w-full rounded-card" />
          <Skeleton className="h-40 w-full rounded-card" />
          <Skeleton className="h-40 w-full rounded-card" />
        </div>
      ) : courses.data && courses.data.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {courses.data.map((course) => (
            <CourseCard
              key={course.id}
              course={course}
              href={`/courses/${course.id}`}
              footer={course.joinCode ? <>Join code <span className="font-mono tracking-wider text-foreground">{course.joinCode}</span></> : null}
            />
          ))}
        </div>
      ) : (
        <EmptyState
          icon={IconBook2}
          title={archived ? "No archived courses" : "No courses yet"}
          description={
            archived
              ? "Courses you archive show up here, read-only."
              : "Create a course for each class or section you teach. Students join with its code."
          }
          action={archived ? undefined : <NewCourseDialog />}
        />
      )}
    </>
  );
}
