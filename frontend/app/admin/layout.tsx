"use client";

import { IconBook2, IconLayoutDashboard, IconUsers } from "@tabler/icons-react";

import { Sidebar } from "@/components/layout/Sidebar";
import { TopNav } from "@/components/layout/TopNav";
import { useAuthGuard } from "@/hooks/useAuthGuard";

const navItems = [
  { href: "/admin/dashboard", label: "Dashboard", icon: IconLayoutDashboard },
  { href: "/admin/users", label: "Users", icon: IconUsers },
  { href: "/admin/courses", label: "Courses", icon: IconBook2 },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const authorized = useAuthGuard("ADMIN");
  if (!authorized) return null;

  return (
    <div className="flex min-h-screen">
      <Sidebar items={navItems} />
      <div className="flex flex-1 flex-col">
        <TopNav profileHref="/admin/profile" />
        <main className="flex-1 p-8">{children}</main>
      </div>
    </div>
  );
}
