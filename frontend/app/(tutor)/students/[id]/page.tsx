"use client";

import { use, useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";

import { SageProgressSummary } from "@/components/sage/SageProgressSummary";
import { SessionCard } from "@/components/session/SessionCard";
import { StudentAvatar } from "@/components/student/StudentAvatar";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useCourses } from "@/hooks/useCourses";
import { useSessions, useCreateSession } from "@/hooks/useSessions";
import { useStudent, useUpdateStudent } from "@/hooks/useStudents";

const editStudentSchema = z.object({
  name: z.string().min(1, "Name is required"),
  email: z.string().email("Enter a valid email address"),
});

type EditStudentValues = z.infer<typeof editStudentSchema>;

function EditStudentDialog({ id, name, email }: { id: string; name: string; email: string }) {
  const [open, setOpen] = useState(false);
  const updateStudent = useUpdateStudent();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditStudentValues>({
    resolver: zodResolver(editStudentSchema),
    values: { name, email },
  });

  function onSubmit(values: EditStudentValues) {
    updateStudent.mutate(
      { id, ...values },
      {
        onSuccess: () => setOpen(false),
      },
    );
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (!nextOpen) reset({ name, email });
      }}
    >
      <DialogTrigger render={<Button variant="outline">Edit</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit student</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="edit-name">Name</Label>
            <Input id="edit-name" {...register("name")} />
            {errors.name ? <p className="text-xs text-destructive">{errors.name.message}</p> : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="edit-email">Email</Label>
            <Input id="edit-email" type="email" {...register("email")} />
            {errors.email ? (
              <p className="text-xs text-destructive">{errors.email.message}</p>
            ) : null}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={updateStudent.isPending}>
              {updateStudent.isPending ? "Saving..." : "Save"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

const scheduleSessionSchema = z.object({
  courseId: z.string().optional(),
  date: z.string().min(1, "Date and time are required"),
  subject: z.string().min(1, "Subject is required"),
  duration: z.coerce.number().int().positive("Duration must be a positive number of minutes"),
});

type ScheduleSessionValues = z.infer<typeof scheduleSessionSchema>;

function ScheduleSessionDialog({ studentId }: { studentId: string }) {
  const [open, setOpen] = useState(false);
  const courses = useCourses();
  const createSession = useCreateSession();
  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<ScheduleSessionValues>({
    resolver: zodResolver(scheduleSessionSchema),
    defaultValues: { courseId: "" },
  });

  function onSubmit(values: ScheduleSessionValues) {
    createSession.mutate(
      { studentId, ...values },
      {
        onSuccess: () => {
          reset();
          setOpen(false);
        },
      },
    );
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (!nextOpen) reset();
      }}
    >
      <DialogTrigger render={<Button>Schedule session</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Schedule session</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="course">Course (optional)</Label>
            <Controller
              control={control}
              name="courseId"
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="course" className="w-full">
                    <SelectValue placeholder="No course">
                      {(value: string | null) => courses.data?.find((c) => c.id === value)?.name}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {courses.data?.map((course) => (
                      <SelectItem key={course.id} value={course.id}>
                        {course.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="subject">Subject</Label>
            <Input id="subject" {...register("subject")} />
            {errors.subject ? (
              <p className="text-xs text-destructive">{errors.subject.message}</p>
            ) : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="date">Date and time</Label>
            <Input id="date" type="datetime-local" {...register("date")} />
            {errors.date ? <p className="text-xs text-destructive">{errors.date.message}</p> : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="duration">Duration (minutes)</Label>
            <Input id="duration" type="number" min={1} {...register("duration")} />
            {errors.duration ? (
              <p className="text-xs text-destructive">{errors.duration.message}</p>
            ) : null}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={createSession.isPending}>
              {createSession.isPending ? "Scheduling..." : "Schedule"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function StudentDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const student = useStudent(id);
  const sessions = useSessions();

  const studentSessions = useMemo(
    () => (sessions.data ?? []).filter((session) => session.studentId === id),
    [sessions.data, id],
  );

  if (student.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (!student.data) {
    return <p className="text-sm text-muted-foreground">Student not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <StudentAvatar name={student.data.name} size="lg" />
          <div>
            <h1 className="text-2xl font-medium text-foreground">{student.data.name}</h1>
            <p className="text-sm text-muted-foreground">{student.data.email}</p>
          </div>
        </div>
        <div className="flex shrink-0 gap-2">
          <EditStudentDialog id={student.data.id} name={student.data.name} email={student.data.email} />
          <ScheduleSessionDialog studentId={student.data.id} />
        </div>
      </div>

      <SageProgressSummary studentId={id} />

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Sessions</h2>
        {sessions.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : studentSessions.length > 0 ? (
          <div className="flex flex-col gap-3">
            {studentSessions.map((session) => (
              <SessionCard key={session.id} session={session} href={`/sessions/${session.id}`} />
            ))}
          </div>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No sessions scheduled yet.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
