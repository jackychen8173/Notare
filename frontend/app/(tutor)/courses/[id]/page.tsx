"use client";

import { use, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";

import { AssignmentCard } from "@/components/assignment/AssignmentCard";
import { AnnouncementsSection } from "@/components/course/AnnouncementsSection";
import { GradeCategoriesManager } from "@/components/course/GradeCategoriesManager";
import { MaterialsSection } from "@/components/course/MaterialsSection";
import { TopicsManager } from "@/components/course/TopicsManager";
import { StudentRow } from "@/components/student/StudentRow";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import type { Assignment } from "@/types/assignment";
import type { Course } from "@/types/course";
import { useCreateAssignment, useCourseAssignments } from "@/hooks/useAssignments";
import {
  useArchiveCourse,
  useCourse,
  useEnrolledStudents,
  useRegenerateJoinCode,
  useRemoveStudent,
  useUnarchiveCourse,
  useUpdateCourse,
} from "@/hooks/useCourses";
import { useGradeCategories } from "@/hooks/useGradeCategories";
import { useTopics } from "@/hooks/useTopics";

const editCourseSchema = z.object({
  name: z.string().min(1, "Name is required"),
  subject: z.string().min(1, "Subject is required"),
  description: z.string().optional(),
});

type EditCourseValues = z.infer<typeof editCourseSchema>;

const NO_TOPIC = "__none__";

function groupAssignmentsByTopic(assignments: Assignment[]): [string, Assignment[]][] {
  const groups = new Map<string, Assignment[]>();
  for (const assignment of assignments) {
    const key = assignment.topicName ?? "Ungrouped";
    const existing = groups.get(key);
    if (existing) {
      existing.push(assignment);
    } else {
      groups.set(key, [assignment]);
    }
  }
  const entries = [...groups.entries()];
  entries.sort((a, b) => (a[0] === "Ungrouped" ? 1 : b[0] === "Ungrouped" ? -1 : 0));
  return entries;
}

const createAssignmentSchema = z.object({
  title: z.string().min(1, "Title is required"),
  description: z.string().optional(),
  dueDate: z.string().min(1, "Due date is required"),
  topicId: z.string().optional(),
  gradeCategoryId: z.string().optional(),
});

type CreateAssignmentValues = z.infer<typeof createAssignmentSchema>;

function NewAssignmentDialog({ courseId }: { courseId: string }) {
  const [open, setOpen] = useState(false);
  const createAssignment = useCreateAssignment(courseId);
  const topics = useTopics(courseId);
  const gradeCategories = useGradeCategories(courseId);
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateAssignmentValues>({ resolver: zodResolver(createAssignmentSchema) });

  function onSubmit(values: CreateAssignmentValues) {
    createAssignment.mutate(
      {
        ...values,
        topicId: values.topicId === NO_TOPIC ? undefined : values.topicId,
        gradeCategoryId: values.gradeCategoryId === NO_TOPIC ? undefined : values.gradeCategoryId,
      },
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
          {topics.data && topics.data.length > 0 ? (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="topicId">Topic (optional)</Label>
              <Controller
                control={control}
                name="topicId"
                render={({ field }) => (
                  <Select value={field.value ?? NO_TOPIC} onValueChange={field.onChange}>
                    <SelectTrigger id="topicId" className="w-full">
                      <SelectValue placeholder="No topic" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NO_TOPIC}>No topic</SelectItem>
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
              <Label htmlFor="gradeCategoryId">Grade category (optional)</Label>
              <Controller
                control={control}
                name="gradeCategoryId"
                render={({ field }) => (
                  <Select value={field.value ?? NO_TOPIC} onValueChange={field.onChange}>
                    <SelectTrigger id="gradeCategoryId" className="w-full">
                      <SelectValue placeholder="No category" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NO_TOPIC}>No category</SelectItem>
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

function JoinCodeCard({ courseId, joinCode }: { courseId: string; joinCode: string | null }) {
  const regenerate = useRegenerateJoinCode(courseId);

  return (
    <Card>
      <CardContent className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs text-muted-foreground">Join code</p>
          <p className="font-mono text-lg tracking-widest text-foreground">{joinCode}</p>
        </div>
        <Button
          variant="outline"
          size="sm"
          disabled={regenerate.isPending}
          onClick={() => regenerate.mutate()}
        >
          {regenerate.isPending ? "Regenerating..." : "Regenerate"}
        </Button>
      </CardContent>
    </Card>
  );
}

function EditCourseDialog({ course }: { course: Course }) {
  const [open, setOpen] = useState(false);
  const updateCourse = useUpdateCourse(course.id);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditCourseValues>({
    resolver: zodResolver(editCourseSchema),
    values: { name: course.name, subject: course.subject, description: course.description ?? "" },
  });

  function onSubmit(values: EditCourseValues) {
    updateCourse.mutate(values, {
      onSuccess: () => setOpen(false),
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
      <DialogTrigger render={<Button variant="outline">Edit course</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit course</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="edit-name">Name</Label>
            <Input id="edit-name" {...register("name")} />
            {errors.name ? <p className="text-xs text-destructive">{errors.name.message}</p> : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="edit-subject">Subject</Label>
            <Input id="edit-subject" {...register("subject")} />
            {errors.subject ? (
              <p className="text-xs text-destructive">{errors.subject.message}</p>
            ) : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="edit-description">Description (optional)</Label>
            <Textarea id="edit-description" rows={3} {...register("description")} />
          </div>
          <DialogFooter>
            <Button type="submit" disabled={updateCourse.isPending}>
              {updateCourse.isPending ? "Saving..." : "Save changes"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ArchiveControl({ course }: { course: Course }) {
  const archiveCourse = useArchiveCourse(course.id);
  const unarchiveCourse = useUnarchiveCourse(course.id);

  if (course.archivedAt) {
    return (
      <Button
        variant="outline"
        disabled={unarchiveCourse.isPending}
        onClick={() => unarchiveCourse.mutate()}
      >
        {unarchiveCourse.isPending ? "Unarchiving..." : "Unarchive"}
      </Button>
    );
  }

  return (
    <Button
      variant="outline"
      disabled={archiveCourse.isPending}
      onClick={() => archiveCourse.mutate()}
    >
      {archiveCourse.isPending ? "Archiving..." : "Archive"}
    </Button>
  );
}

export default function CourseDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const course = useCourse(id);
  const enrolled = useEnrolledStudents(id);
  const assignments = useCourseAssignments(id);
  const removeStudent = useRemoveStudent(id);

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

  const archived = course.data.archivedAt !== null;

  return (
    <div className="flex flex-col gap-6">
      {archived ? (
        <Alert>
          <AlertTitle>This course is archived</AlertTitle>
          <AlertDescription>
            It&apos;s read-only — no new assignments, sessions, or enrollments. Unarchive to make changes again.
          </AlertDescription>
        </Alert>
      ) : null}

      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-medium text-foreground">{course.data.name}</h1>
          <p className="text-sm text-muted-foreground">{course.data.subject}</p>
          {course.data.description ? (
            <p className="mt-2 text-sm text-muted-foreground">{course.data.description}</p>
          ) : null}
          <div className="mt-3 flex gap-2">
            <EditCourseDialog course={course.data} />
            <ArchiveControl course={course.data} />
          </div>
        </div>
        {!archived ? <JoinCodeCard courseId={id} joinCode={course.data.joinCode} /> : null}
      </div>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Enrolled students</h2>
        {enrolled.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : enrolled.data && enrolled.data.length > 0 ? (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Added</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {enrolled.data.map((student) => (
                <StudentRow
                  key={student.id}
                  student={student}
                  onRemove={() => removeStudent.mutate(student.id)}
                  removePending={removeStudent.isPending && removeStudent.variables === student.id}
                />
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
          {!archived ? <NewAssignmentDialog courseId={id} /> : null}
        </div>
        {assignments.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : assignments.data && assignments.data.length > 0 ? (
          <div className="flex flex-col gap-4">
            {groupAssignmentsByTopic(assignments.data).map(([topicName, group]) => (
              <div key={topicName}>
                <p className="mb-2 text-xs font-medium text-muted-foreground">{topicName}</p>
                <div className="grid gap-3 sm:grid-cols-2">
                  {group.map((assignment) => (
                    <AssignmentCard key={assignment.id} assignment={assignment} />
                  ))}
                </div>
              </div>
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

      <TopicsManager courseId={id} editable={!archived} />

      <MaterialsSection courseId={id} editable={!archived} />

      <GradeCategoriesManager courseId={id} editable={!archived} />

      <AnnouncementsSection courseId={id} editable={!archived} />
    </div>
  );
}
