"use client";

import Link from "next/link";

import { PageHeader } from "@/components/layout/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyCourses } from "@/hooks/useCourses";

export default function StudentCoursesPage() {
  const courses = useMyCourses();

  return (
    <>
      <PageHeader title="Courses" description="Courses you're enrolled in." />

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
