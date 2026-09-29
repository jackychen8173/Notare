"use client";

import { IconBook2, IconCalendar, IconCode, IconHome } from "@tabler/icons-react";

import { AppShell } from "@/components/layout/AppShell";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { useMyCourses } from "@/hooks/useCourses";

const navItems = [
  { href: "/student/dashboard", label: "Home", icon: IconHome, shortcut: "h" },
  { href: "/student/courses", label: "Courses", icon: IconBook2, shortcut: "c" },
  { href: "/student/workspace", label: "Workspace", icon: IconCode, shortcut: "w" },
  { href: "/student/calendar", label: "Calendar", icon: IconCalendar, shortcut: "l" },
];

export default function StudentLayout({ children }: { children: React.ReactNode }) {
  const authorized = useAuthGuard("STUDENT");
  const courses = useMyCourses({ enabled: authorized });
  if (!authorized) return null;

  return (
    <AppShell
      role="student"
      navItems={navItems}
      courses={{ basePath: "/student/courses", items: courses.data }}
      profileHref="/student/profile"
    >
      {children}
    </AppShell>
  );
}
