"use client";

import { use, useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";

import { AssignmentCard } from "@/components/assignment/AssignmentCard";
import { StudentRow } from "@/components/student/StudentRow";
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
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { useCreateAssignment, useCourseAssignments } from "@/hooks/useAssignments";
import { useCourse, useEnrolledStudents, useEnrollStudent } from "@/hooks/useCourses";
import { useStudents } from "@/hooks/useStudents";

const enrollSchema = z.object({
  studentId: z.string().min(1, "Choose a student"),
});

type EnrollValues = z.infer<typeof enrollSchema>;

function EnrollStudentDialog({ courseId }: { courseId: string }) {
  const [open, setOpen] = useState(false);
  const students = useStudents();
  const enrolled = useEnrolledStudents(courseId);
  const enrollStudent = useEnrollStudent(courseId);
  const {
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<EnrollValues>({ resolver: zodResolver(enrollSchema) });

  const enrolledIds = useMemo(() => new Set((enrolled.data ?? []).map((s) => s.id)), [enrolled.data]);
  const available = (students.data ?? []).filter((s) => !enrolledIds.has(s.id));

  function onSubmit(values: EnrollValues) {
    enrollStudent.mutate(values.studentId, {
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
      <DialogTrigger render={<Button variant="outline">Enroll student</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Enroll student</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="student">Student</Label>
            <Controller
              control={control}
              name="studentId"
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="student" className="w-full">
                    <SelectValue placeholder="Choose a student" />
                  </SelectTrigger>
                  <SelectContent>
                    {available.map((s) => (
                      <SelectItem key={s.id} value={s.id}>
                        {s.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.studentId ? (
              <p className="text-xs text-destructive">{errors.studentId.message}</p>
            ) : null}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={enrollStudent.isPending}>
              {enrollStudent.isPending ? "Enrolling..." : "Enroll"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

const createAssignmentSchema = z.object({
  title: z.string().min(1, "Title is required"),
  description: z.string().optional(),
  dueDate: z.string().min(1, "Due date is required"),
});

type CreateAssignmentValues = z.infer<typeof createAssignmentSchema>;

function NewAssignmentDialog({ courseId }: { courseId: string }) {
  const [open, setOpen] = useState(false);
  const createAssignment = useCreateAssignment(courseId);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateAssignmentValues>({ resolver: zodResolver(createAssignmentSchema) });

  function onSubmit(values: CreateAssignmentValues) {
    createAssignment.mutate(values, {
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
      <DialogTrigger render={<Button variant="outline">New assignment</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New assignment</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="title">Title</Label>
            <Input id="title" {...register("title")} />
            {errors.title ? (
              <p className="text-xs text-destructive">{errors.title.message}</p>
            ) : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="description">Description (optional)</Label>
            <Textarea id="description" rows={3} {...register("description")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="dueDate">Due date</Label>
            <Input id="dueDate" type="date" {...register("dueDate")} />
            {errors.dueDate ? (
              <p className="text-xs text-destructive">{errors.dueDate.message}</p>
            ) : null}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={createAssignment.isPending}>
              {createAssignment.isPending ? "Creating..." : "Create assignment"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function CourseDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const course = useCourse(id);
  const enrolled = useEnrolledStudents(id);
  const assignments = useCourseAssignments(id);

  if (course.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (!course.data) {
    return <p className="text-sm text-muted-foreground">Course not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-medium text-foreground">{course.data.name}</h1>
        <p className="text-sm text-muted-foreground">{course.data.subject}</p>
        {course.data.description ? (
          <p className="mt-2 text-sm text-muted-foreground">{course.data.description}</p>
        ) : null}
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-medium text-foreground">Enrolled students</h2>
          <EnrollStudentDialog courseId={id} />
        </div>
        {enrolled.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : enrolled.data && enrolled.data.length > 0 ? (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Added</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {enrolled.data.map((student) => (
                <StudentRow key={student.id} student={student} />
              ))}
            </TableBody>
          </Table>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No students enrolled yet.</p>
            </CardContent>
          </Card>
        )}
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-medium text-foreground">Assignments</h2>
          <NewAssignmentDialog courseId={id} />
        </div>
        {assignments.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : assignments.data && assignments.data.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2">
            {assignments.data.map((assignment) => (
              <AssignmentCard key={assignment.id} assignment={assignment} />
            ))}
          </div>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No assignments yet.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
