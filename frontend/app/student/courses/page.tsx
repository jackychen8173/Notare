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
import { useJoinCourse, useMyCourses } from "@/hooks/useCourses";

const joinCourseSchema = z.object({
  code: z.string().min(1, "Enter a join code"),
});

type JoinCourseValues = z.infer<typeof joinCourseSchema>;

function JoinCourseDialog() {
  const [open, setOpen] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const joinCourse = useJoinCourse();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<JoinCourseValues>({ resolver: zodResolver(joinCourseSchema) });

  function onSubmit(values: JoinCourseValues) {
    setServerError(null);
    joinCourse.mutate(values.code, {
      onSuccess: () => {
        reset();
        setOpen(false);
      },
      onError: () => {
        setServerError("That code didn't work. Check it and try again.");
      },
    });
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (!nextOpen) {
          reset();
          setServerError(null);
        }
      }}
    >
      <DialogTrigger render={<Button>Join a class</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Join a class</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="code">Class code</Label>
            <Input id="code" autoCapitalize="characters" {...register("code")} />
            {errors.code ? (
              <p className="text-xs text-destructive">{errors.code.message}</p>
            ) : null}
            {serverError ? <p className="text-xs text-destructive">{serverError}</p> : null}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={joinCourse.isPending}>
              {joinCourse.isPending ? "Joining..." : "Join"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function StudentCoursesPage() {
  const courses = useMyCourses();

  return (
    <>
      <PageHeader
        title="Courses"
        description="Courses you're enrolled in."
        actions={<JoinCourseDialog />}
      />

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
              href={`/student/courses/${course.id}`}
              meta={course.tutorName}
            />
          ))}
        </div>
      ) : (
        <EmptyState
          icon={IconBook2}
          title="You're not in any courses yet"
          description="Ask your tutor for the class code, then join with it."
          action={<JoinCourseDialog />}
        />
      )}
    </>
  );
}
