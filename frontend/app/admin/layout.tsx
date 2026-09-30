"use client";

import { IconBook2, IconLayoutDashboard, IconMessageReport, IconUsers } from "@tabler/icons-react";

import { AppShell } from "@/components/layout/AppShell";
import { useAuthGuard } from "@/hooks/useAuthGuard";

const navItems = [
  { href: "/admin/dashboard", label: "Dashboard", icon: IconLayoutDashboard, shortcut: "d" },
  { href: "/admin/users", label: "Users", icon: IconUsers, shortcut: "u" },
  { href: "/admin/courses", label: "Courses", icon: IconBook2, shortcut: "c" },
  { href: "/admin/reports", label: "Reports", icon: IconMessageReport, shortcut: "r" },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const authorized = useAuthGuard("ADMIN");
  if (!authorized) return null;

  return (
    <AppShell role="admin" navItems={navItems} profileHref="/admin/profile">
      {children}
    </AppShell>
  );
}
