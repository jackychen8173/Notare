"use client";

import Link from "next/link";
import { IconBook2, IconCalendarEvent } from "@tabler/icons-react";

import { PageHeader } from "@/components/layout/PageHeader";
import { SessionCard } from "@/components/session/SessionCard";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyCourses } from "@/hooks/useCourses";
import { useMySessions } from "@/hooks/useSessions";

export default function StudentDashboardPage() {
  const courses = useMyCourses();
  const sessions = useMySessions();

  const upcoming = (sessions.data ?? [])
    .filter((s) => s.status === "SCHEDULED")
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
    .slice(0, 3);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Dashboard" description="Your courses and upcoming sessions." />

      <div className="grid gap-4 sm:grid-cols-2">
        <Link href="/student/courses">
          <Card className="transition-colors hover:bg-muted/40">
            <CardContent className="flex items-center gap-4">
              <div className="flex size-10 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                <IconBook2 className="size-5" stroke={1.75} />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Courses</p>
                {courses.isLoading ? (
                  <Skeleton className="mt-1 h-6 w-10" />
                ) : (
                  <p className="text-xl font-medium text-foreground">{courses.data?.length ?? 0}</p>
                )}
              </div>
            </CardContent>
          </Card>
        </Link>

        <Card>
          <CardContent className="flex items-center gap-4">
            <div className="flex size-10 items-center justify-center rounded-lg bg-muted text-muted-foreground">
              <IconCalendarEvent className="size-5" stroke={1.75} />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Upcoming sessions</p>
              {sessions.isLoading ? (
                <Skeleton className="mt-1 h-6 w-10" />
              ) : (
                <p className="text-xl font-medium text-foreground">
                  {(sessions.data ?? []).filter((s) => s.status === "SCHEDULED").length}
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Next sessions</h2>
        {sessions.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : upcoming.length > 0 ? (
          <div className="flex flex-col gap-3">
            {upcoming.map((session) => (
              <SessionCard key={session.id} session={session} />
            ))}
          </div>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No upcoming sessions.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
