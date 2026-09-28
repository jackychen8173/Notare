"use client";

import { IconBook2, IconCalendar, IconHome, IconMessageChatbot, IconUsers } from "@tabler/icons-react";

import { AppShell } from "@/components/layout/AppShell";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { useCourses } from "@/hooks/useCourses";

const navItems = [
  { href: "/dashboard", label: "Home", icon: IconHome },
  { href: "/students", label: "Students", icon: IconUsers },
  { href: "/courses", label: "Courses", icon: IconBook2 },
  { href: "/calendar", label: "Calendar", icon: IconCalendar },
  { href: "/sage", label: "Ask Sage", icon: IconMessageChatbot },
];

export default function TutorLayout({ children }: { children: React.ReactNode }) {
  const authorized = useAuthGuard("TUTOR");
  const courses = useCourses(false, { enabled: authorized });
  if (!authorized) return null;

  return (
    <AppShell role="tutor" navItems={navItems} courses={{ basePath: "/courses", items: courses.data }} profileHref="/profile">
      {children}
    </AppShell>
  );
}
