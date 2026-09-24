"use client";

import { use } from "react";

import { AssignmentCard } from "@/components/assignment/AssignmentCard";
import { StudentAnnouncementsSection } from "@/components/course/AnnouncementsSection";
import { StudentMaterialsSection } from "@/components/course/MaterialsSection";
import { QuizCard } from "@/components/quiz/QuizCard";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyCourseAssignments } from "@/hooks/useAssignments";
import { useMyCourse } from "@/hooks/useCourses";
import { useMyCourseQuizzes } from "@/hooks/useQuizzes";

export default function StudentCourseDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const course = useMyCourse(id);
  const assignments = useMyCourseAssignments(id);
  const quizzes = useMyCourseQuizzes(id);

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
        <h2 className="mb-3 text-lg font-medium text-foreground">Assignments</h2>
        {assignments.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : assignments.data && assignments.data.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2">
            {assignments.data.map((assignment) => (
              <AssignmentCard
                key={assignment.id}
                assignment={assignment}
                href={`/student/assignments/${assignment.id}`}
              />
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

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Quizzes</h2>
        {quizzes.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : quizzes.data && quizzes.data.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2">
            {quizzes.data.map((quiz) => (
              <QuizCard key={quiz.id} quiz={quiz} href={`/student/quizzes/${quiz.id}`} />
            ))}
          </div>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No quizzes yet.</p>
            </CardContent>
          </Card>
        )}
      </div>

      <StudentMaterialsSection courseId={id} />

      <StudentAnnouncementsSection courseId={id} />
    </div>
  );
}
