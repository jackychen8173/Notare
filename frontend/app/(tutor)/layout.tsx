"use client";

import { IconBook2, IconLayoutDashboard, IconUsers } from "@tabler/icons-react";

import { Sidebar } from "@/components/layout/Sidebar";
import { TopNav } from "@/components/layout/TopNav";
import { useAuthGuard } from "@/hooks/useAuthGuard";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: IconLayoutDashboard },
  { href: "/students", label: "Students", icon: IconUsers },
  { href: "/courses", label: "Courses", icon: IconBook2 },
];

export default function TutorLayout({ children }: { children: React.ReactNode }) {
  const authorized = useAuthGuard("TUTOR");
  if (!authorized) return null;

  return (
    <div className="flex min-h-screen">
      <Sidebar items={navItems} />
      <div className="flex flex-1 flex-col">
        <TopNav profileHref="/profile" />
        <main className="flex-1 p-8">{children}</main>
      </div>
    </div>
  );
}
