"use client";

import Link from "next/link";
import { IconBook2, IconClipboardCheck, IconUsers } from "@tabler/icons-react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCourses } from "@/hooks/useCourses";
import { useStudents } from "@/hooks/useStudents";
import { usePendingReviews } from "@/hooks/useSubmissions";

function StatValue({ isLoading, value }: { isLoading: boolean; value: number | undefined }) {
  if (isLoading) return <Skeleton className="mt-1 h-6 w-10" />;
  return <p className="text-xl font-medium text-foreground">{value ?? 0}</p>;
}

export default function DashboardPage() {
  const students = useStudents();
  const courses = useCourses();
  const pendingReviews = usePendingReviews();

  return (
    <>
      <PageHeader
        title="Dashboard"
        description="Overview of your students, courses, and pending reviews."
      />
      <div className="grid gap-4 sm:grid-cols-3">
        <Link href="/students">
          <Card className="transition-colors hover:bg-muted/40">
            <CardContent className="flex items-center gap-4">
              <div className="flex size-10 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                <IconUsers className="size-5" stroke={1.75} />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Students</p>
                <StatValue isLoading={students.isLoading} value={students.data?.length} />
              </div>
            </CardContent>
          </Card>
        </Link>

        <Link href="/courses">
          <Card className="transition-colors hover:bg-muted/40">
            <CardContent className="flex items-center gap-4">
              <div className="flex size-10 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                <IconBook2 className="size-5" stroke={1.75} />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Courses</p>
                <StatValue isLoading={courses.isLoading} value={courses.data?.length} />
              </div>
            </CardContent>
          </Card>
        </Link>

        <Card>
          <CardContent className="flex items-center gap-4">
            <div className="flex size-10 items-center justify-center rounded-lg bg-sage-surface text-sage">
              <IconClipboardCheck className="size-5" stroke={1.75} />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Pending reviews</p>
              <StatValue isLoading={pendingReviews.isLoading} value={pendingReviews.data?.count} />
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  );
}
