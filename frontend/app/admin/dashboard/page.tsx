"use client";

import { IconBook2, IconCircleCheck, IconClock, IconSchool, IconUsers } from "@tabler/icons-react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminDashboard } from "@/hooks/useAdminDashboard";

function StatCard({
  icon: Icon,
  label,
  value,
  isLoading,
}: {
  icon: typeof IconUsers;
  label: string;
  value: number | undefined;
  isLoading: boolean;
}) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4">
        <div className="flex size-10 items-center justify-center rounded-lg bg-muted text-muted-foreground">
          <Icon className="size-5" stroke={1.75} />
        </div>
        <div>
          <p className="text-sm text-muted-foreground">{label}</p>
          {isLoading ? (
            <Skeleton className="mt-1 h-6 w-10" />
          ) : (
            <p className="text-xl font-medium text-foreground">{value ?? 0}</p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

export default function AdminDashboardPage() {
  const dashboard = useAdminDashboard();

  return (
    <>
      <PageHeader title="Dashboard" description="Platform-wide overview of every tutor and student." />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard icon={IconSchool} label="Tutors" value={dashboard.data?.tutorCount} isLoading={dashboard.isLoading} />
        <StatCard icon={IconUsers} label="Students" value={dashboard.data?.studentCount} isLoading={dashboard.isLoading} />
        <StatCard icon={IconBook2} label="Courses" value={dashboard.data?.courseCount} isLoading={dashboard.isLoading} />
        <StatCard
          icon={IconClock}
          label="Submissions pending"
          value={dashboard.data?.submissionsPending}
          isLoading={dashboard.isLoading}
        />
        <StatCard
          icon={IconCircleCheck}
          label="Submissions released"
          value={dashboard.data?.submissionsReleased}
          isLoading={dashboard.isLoading}
        />
      </div>
    </>
  );
}
