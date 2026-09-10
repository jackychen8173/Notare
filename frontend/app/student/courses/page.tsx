"use client";

import { useState } from "react";
import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { PageHeader } from "@/components/layout/PageHeader";
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
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      ) : courses.data && courses.data.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {courses.data.map((course) => (
            <Link key={course.id} href={`/student/courses/${course.id}`}>
              <Card className="transition-colors hover:bg-muted/40">
                <CardContent>
                  <p className="font-medium text-foreground">{course.name}</p>
                  <p className="text-sm text-muted-foreground">{course.subject}</p>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">You&apos;re not enrolled in any courses yet.</p>
      )}
    </>
  );
}
