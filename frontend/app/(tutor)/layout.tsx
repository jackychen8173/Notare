"use client";

import { IconBook2, IconLayoutDashboard, IconMessageChatbot, IconUsers } from "@tabler/icons-react";

import { AppShell } from "@/components/layout/AppShell";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { useCourses } from "@/hooks/useCourses";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: IconLayoutDashboard },
  { href: "/students", label: "Students", icon: IconUsers },
  { href: "/courses", label: "Courses", icon: IconBook2 },
  { href: "/sage", label: "Ask Sage", icon: IconMessageChatbot },
];

export default function TutorLayout({ children }: { children: React.ReactNode }) {
  const authorized = useAuthGuard("TUTOR");
  const courses = useCourses(false, { enabled: authorized });
  if (!authorized) return null;

  return (
    <AppShell navItems={navItems} courses={{ basePath: "/courses", items: courses.data }} profileHref="/profile">
      {children}
    </AppShell>
  );
}
