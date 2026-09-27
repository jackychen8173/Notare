"use client";

import { IconBook2, IconCalendar, IconLayoutDashboard } from "@tabler/icons-react";

import { AppShell } from "@/components/layout/AppShell";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { useMyCourses } from "@/hooks/useCourses";

const navItems = [
  { href: "/student/dashboard", label: "Dashboard", icon: IconLayoutDashboard },
  { href: "/student/courses", label: "Courses", icon: IconBook2 },
  { href: "/student/calendar", label: "Calendar", icon: IconCalendar },
];

export default function StudentLayout({ children }: { children: React.ReactNode }) {
  const authorized = useAuthGuard("STUDENT");
  const courses = useMyCourses({ enabled: authorized });
  if (!authorized) return null;

  return (
    <AppShell
      navItems={navItems}
      courses={{ basePath: "/student/courses", items: courses.data }}
      profileHref="/student/profile"
    >
      {children}
    </AppShell>
  );
}
