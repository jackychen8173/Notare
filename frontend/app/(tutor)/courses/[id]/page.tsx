"use client";

import { Suspense, use, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { IconCheck, IconCopy, IconRefresh, IconUsers } from "@tabler/icons-react";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";

import { AnnouncementsSection } from "@/components/course/AnnouncementsSection";
import { ClassworkByTopic } from "@/components/course/ClassworkByTopic";
import { CourseBanner } from "@/components/course/CourseBanner";
import { CourseColorPicker } from "@/components/course/CourseColorPicker";
import { CourseScheduleFields } from "@/components/course/CourseScheduleFields";
import { CourseTabs, useCourseTab, type CourseTab } from "@/components/course/CourseTabs";
import { GradeCategoriesManager } from "@/components/course/GradeCategoriesManager";
import { MaterialsSection } from "@/components/course/MaterialsSection";
import { TopicsManager } from "@/components/course/TopicsManager";
import { UpcomingCard } from "@/components/course/UpcomingCard";
import { DiscussionsSection } from "@/components/discussion/DiscussionsSection";
import { Breadcrumbs } from "@/components/layout/Breadcrumbs";
import { EmptyState } from "@/components/layout/EmptyState";
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
import { COURSE_COLORS, type Course } from "@/types/course";
import { useCreateAssignment, useCourseAssignments } from "@/hooks/useAssignments";
import {
  useArchiveCourse,
  useCourse,
  useDuplicateCourse,
  useEnrolledStudents,
  useRegenerateJoinCode,
  useRemoveStudent,
  useUnarchiveCourse,
  useUpdateCourse,
} from "@/hooks/useCourses";
import { useDiscussionThreads } from "@/hooks/useDiscussions";
import { useGradeCategories } from "@/hooks/useGradeCategories";
import { useCourseQuizzes, useCreateQuiz } from "@/hooks/useQuizzes";
import { useTopics } from "@/hooks/useTopics";
import { formToSchedule, scheduleFormSchema, scheduleToForm } from "@/lib/schedule";

const editCourseSchema = z.object({
  name: z.string().min(1, "Name is required"),
  subject: z.string().min(1, "Subject is required"),
  description: z.string().optional(),
  color: z.enum(COURSE_COLORS as [Course["color"], ...Course["color"][]]),
  schedule: scheduleFormSchema,
});

type EditCourseValues = z.infer<typeof editCourseSchema>;

const NO_TOPIC = "__none__";

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

const createQuizSchema = z.object({
  title: z.string().min(1, "Title is required"),
  description: z.string().optional(),
  timeLimitMinutes: z.string().optional(),
  topicId: z.string().optional(),
  gradeCategoryId: z.string().optional(),
});

type CreateQuizValues = z.infer<typeof createQuizSchema>;

function NewQuizDialog({ courseId }: { courseId: string }) {
  const [open, setOpen] = useState(false);
  const router = useRouter();
  const createQuiz = useCreateQuiz(courseId);
  const topics = useTopics(courseId);
  const gradeCategories = useGradeCategories(courseId);
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateQuizValues>({ resolver: zodResolver(createQuizSchema) });

  function onSubmit(values: CreateQuizValues) {
    createQuiz.mutate(
      {
        title: values.title,
        description: values.description || undefined,
        timeLimitMinutes: values.timeLimitMinutes ? Number(values.timeLimitMinutes) : undefined,
        topicId: values.topicId === NO_TOPIC ? undefined : values.topicId,
        gradeCategoryId: values.gradeCategoryId === NO_TOPIC ? undefined : values.gradeCategoryId,
      },
      {
        // Question-building happens on the quiz's own detail page, not in this dialog -
        // navigate there right after the quiz shell is created.
        onSuccess: (quiz) => {
          reset();
          setOpen(false);
          router.push(`/quizzes/${quiz.id}`);
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
      <DialogTrigger render={<Button variant="outline">New quiz</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New quiz</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="quiz-title">Title</Label>
            <Input id="quiz-title" {...register("title")} />
            {errors.title ? (
              <p className="text-xs text-destructive">{errors.title.message}</p>
            ) : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="quiz-description">Description (optional)</Label>
            <Textarea id="quiz-description" rows={3} {...register("description")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="quiz-time-limit">Time limit, minutes (optional)</Label>
            <Input id="quiz-time-limit" type="number" min={1} {...register("timeLimitMinutes")} />
            <p className="text-xs text-muted-foreground">
              Leave blank for an untimed quiz. A timed quiz auto-submits when time runs out.
            </p>
          </div>
          {topics.data && topics.data.length > 0 ? (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="quiz-topicId">Topic (optional)</Label>
              <Controller
                control={control}
                name="topicId"
                render={({ field }) => (
                  <Select value={field.value ?? NO_TOPIC} onValueChange={field.onChange}>
                    <SelectTrigger id="quiz-topicId" className="w-full">
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
              <Label htmlFor="quiz-gradeCategoryId">Grade category (optional)</Label>
              <Controller
                control={control}
                name="gradeCategoryId"
                render={({ field }) => (
                  <Select value={field.value ?? NO_TOPIC} onValueChange={field.onChange}>
                    <SelectTrigger id="quiz-gradeCategoryId" className="w-full">
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
            <Button type="submit" disabled={createQuiz.isPending}>
              {createQuiz.isPending ? "Creating..." : "Create quiz"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function JoinCodeChip({ courseId, joinCode }: { courseId: string; joinCode: string | null }) {
  const regenerate = useRegenerateJoinCode(courseId);
  const [copied, setCopied] = useState(false);

  function copy() {
    if (!joinCode) return;
    navigator.clipboard?.writeText(joinCode).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  }

  return (
    <div className="flex items-center gap-1 rounded-card bg-white/15 p-1.5 pl-3 backdrop-blur-sm">
      <div className="mr-2">
        <p className="text-[11px] font-medium tracking-wide text-white/80 uppercase">Join code</p>
        <p className="font-mono text-lg font-medium tracking-widest text-white">{joinCode}</p>
      </div>
      <Button
        variant="ghost"
        size="icon-sm"
        className="text-white hover:bg-white/20 hover:text-white"
        aria-label={copied ? "Copied" : "Copy join code"}
        onClick={copy}
      >
        {copied ? <IconCheck stroke={2} /> : <IconCopy stroke={1.75} />}
      </Button>
      <Button
        variant="ghost"
        size="icon-sm"
        className="text-white hover:bg-white/20 hover:text-white"
        aria-label="Regenerate join code"
        title="Regenerate join code"
        disabled={regenerate.isPending}
        onClick={() => regenerate.mutate()}
      >
        <IconRefresh stroke={1.75} />
      </Button>
    </div>
  );
}

function EditCourseDialog({ course }: { course: Course }) {
  const [open, setOpen] = useState(false);
  const updateCourse = useUpdateCourse(course.id);
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditCourseValues>({
    resolver: zodResolver(editCourseSchema),
    values: {
      name: course.name,
      subject: course.subject,
      description: course.description ?? "",
      color: course.color,
      schedule: scheduleToForm(course.schedule),
    },
  });

  function onSubmit({ schedule, ...values }: EditCourseValues) {
    updateCourse.mutate({ ...values, schedule: formToSchedule(schedule) }, {
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
      <DialogTrigger render={<Button variant="outline">Edit details</Button>} />
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-md">
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
          <div className="flex flex-col gap-2">
            <Label>Color</Label>
            <Controller
              control={control}
              name="color"
              render={({ field }) => <CourseColorPicker value={field.value} onChange={field.onChange} />}
            />
          </div>
          <Controller
            control={control}
            name="schedule"
            render={({ field }) => (
              <CourseScheduleFields
                idPrefix="edit-course"
                value={field.value}
                onChange={field.onChange}
                error={errors.schedule?.message}
              />
            )}
          />
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

const duplicateCourseSchema = z.object({
  name: z.string().min(1, "Name is required"),
});

type DuplicateCourseValues = z.infer<typeof duplicateCourseSchema>;

function DuplicateCourseDialog({ course }: { course: Course }) {
  const [open, setOpen] = useState(false);
  const router = useRouter();
  const duplicateCourse = useDuplicateCourse(course.id);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DuplicateCourseValues>({
    resolver: zodResolver(duplicateCourseSchema),
    values: { name: `${course.name} (copy)` },
  });

  function onSubmit(values: DuplicateCourseValues) {
    duplicateCourse.mutate(values.name, {
      onSuccess: (copy) => {
        setOpen(false);
        router.push(`/courses/${copy.id}`);
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
      <DialogTrigger render={<Button variant="outline">Duplicate</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Duplicate course</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <p className="text-sm text-muted-foreground">
            Creates a separate course with its own join code, for another section or a new school year.
            Topics, materials, grade categories, assignments, rubrics, and quizzes are copied. Students,
            submissions, announcements, and discussions are not. Due dates are kept as-is, so update them
            if needed.
          </p>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="duplicate-name">New course name</Label>
            <Input id="duplicate-name" {...register("name")} />
            {errors.name ? <p className="text-xs text-destructive">{errors.name.message}</p> : null}
          </div>
          {duplicateCourse.isError ? (
            <p className="text-xs text-destructive">Couldn&apos;t duplicate the course. Please try again.</p>
          ) : null}
          <DialogFooter>
            <Button type="submit" disabled={duplicateCourse.isPending}>
              {duplicateCourse.isPending ? "Duplicating..." : "Duplicate course"}
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
      variant="destructive"
      disabled={archiveCourse.isPending}
      onClick={() => archiveCourse.mutate()}
    >
      {archiveCourse.isPending ? "Archiving..." : "Archive course"}
    </Button>
  );
}

function SettingsRow({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-3 py-4 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="text-sm font-medium text-foreground">{title}</p>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      <div className="shrink-0">{children}</div>
    </div>
  );
}

function PeopleTab({ courseId }: { courseId: string }) {
  const enrolled = useEnrolledStudents(courseId);
  const removeStudent = useRemoveStudent(courseId);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-baseline justify-between">
        <h2 className="text-lg font-semibold text-foreground">Students</h2>
        {enrolled.data ? (
          <p className="text-sm text-muted-foreground">
            {enrolled.data.length} enrolled
          </p>
        ) : null}
      </div>
      {enrolled.isLoading ? (
        <Skeleton className="h-16 w-full" />
      ) : enrolled.data && enrolled.data.length > 0 ? (
        <Card className="py-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">Name</TableHead>
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
        </Card>
      ) : (
        <EmptyState
          icon={IconUsers}
          title="No students yet"
          description="Share the join code above. Students enter it under Courses → Join a class."
        />
      )}
    </div>
  );
}

const TABS: CourseTab[] = [
  { id: "stream", label: "Stream" },
  { id: "classwork", label: "Classwork" },
  { id: "people", label: "People" },
  { id: "discussions", label: "Discussions" },
  { id: "settings", label: "Settings" },
];

function CourseDetail({ id }: { id: string }) {
  const course = useCourse(id);
  const assignments = useCourseAssignments(id);
  const quizzes = useCourseQuizzes(id);
  const topics = useTopics(id);
  const threads = useDiscussionThreads("tutor", id);
  const unread = threads.data?.filter((thread) => thread.unread).length ?? 0;
  const tabs = TABS.map((tab) => (tab.id === "discussions" ? { ...tab, badge: unread } : tab));
  const tab = useCourseTab(tabs);

  if (course.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-4 w-40" />
        <Skeleton className="h-36 w-full rounded-card" />
        <Skeleton className="h-10 w-full" />
      </div>
    );
  }

  if (!course.data) {
    return <p className="text-sm text-muted-foreground">Course not found.</p>;
  }

  const archived = course.data.archivedAt !== null;
  const assignmentList = assignments.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Breadcrumbs items={[{ label: "Courses", href: "/courses" }, { label: course.data.name }]} />
        {archived ? (
          <Alert className="mb-4">
            <AlertTitle>This course is archived</AlertTitle>
            <AlertDescription>
              It&apos;s read-only — no new assignments, sessions, or enrollments. Unarchive it under Settings to make
              changes again.
            </AlertDescription>
          </Alert>
        ) : null}
        <CourseBanner
          course={course.data}
          aside={!archived ? <JoinCodeChip courseId={id} joinCode={course.data.joinCode} /> : null}
        />
      </div>

      <CourseTabs tabs={tabs} active={tab} />

      {tab === "stream" ? (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
          <AnnouncementsSection courseId={id} editable={!archived} />
          <div className="flex flex-col gap-4">
            <UpcomingCard assignments={assignmentList} href={(assignment) => `/assignments/${assignment.id}`} />
          </div>
        </div>
      ) : null}

      {tab === "classwork" ? (
        <div className="flex flex-col gap-10">
          <div className="flex flex-col gap-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-semibold text-foreground">Assignments &amp; quizzes</h2>
              {!archived ? (
                <div className="flex gap-2">
                  <NewAssignmentDialog courseId={id} />
                  <NewQuizDialog courseId={id} />
                </div>
              ) : null}
            </div>
            {assignments.isLoading || quizzes.isLoading ? (
              <Skeleton className="h-24 w-full" />
            ) : (
              <ClassworkByTopic
                assignments={assignmentList}
                quizzes={quizzes.data ?? []}
                topicOrder={topics.data?.map((topic) => topic.name)}
                emptyDescription="Create an assignment or quiz. Group them by unit with topics below."
              />
            )}
          </div>
          <MaterialsSection courseId={id} editable={!archived} />
          <TopicsManager courseId={id} editable={!archived} />
        </div>
      ) : null}

      {tab === "people" ? <PeopleTab courseId={id} /> : null}

      {tab === "discussions" ? <DiscussionsSection scope="tutor" courseId={id} archived={archived} /> : null}

      {tab === "settings" ? (
        <div className="flex flex-col gap-10">
          <Card>
            <CardContent className="divide-y divide-border">
              <SettingsRow title="Course details" description="Name, subject, description, and color.">
                <EditCourseDialog course={course.data} />
              </SettingsRow>
              <SettingsRow
                title="Duplicate course"
                description="Copy this course's content into a new course for another section or school year."
              >
                <DuplicateCourseDialog course={course.data} />
              </SettingsRow>
              <SettingsRow
                title={archived ? "Unarchive course" : "Archive course"}
                description={
                  archived
                    ? "Make the course editable again and let students join."
                    : "Make the course read-only and stop new enrollments. You can undo this."
                }
              >
                <ArchiveControl course={course.data} />
              </SettingsRow>
            </CardContent>
          </Card>
          <GradeCategoriesManager courseId={id} editable={!archived} />
        </div>
      ) : null}
    </div>
  );
}

export default function CourseDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  // useSearchParams (the active tab) needs a Suspense boundary.
  return (
    <Suspense fallback={<Skeleton className="h-36 w-full rounded-card" />}>
      <CourseDetail id={id} />
    </Suspense>
  );
}
