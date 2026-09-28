"use client";

import { IconBook2, IconLayoutDashboard, IconMessageReport, IconUsers } from "@tabler/icons-react";

import { AppShell } from "@/components/layout/AppShell";
import { useAuthGuard } from "@/hooks/useAuthGuard";

const navItems = [
  { href: "/admin/dashboard", label: "Dashboard", icon: IconLayoutDashboard },
  { href: "/admin/users", label: "Users", icon: IconUsers },
  { href: "/admin/courses", label: "Courses", icon: IconBook2 },
  { href: "/admin/reports", label: "Reports", icon: IconMessageReport },
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
