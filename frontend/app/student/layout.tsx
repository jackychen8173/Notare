"use client";

import { IconBook2, IconLayoutDashboard } from "@tabler/icons-react";

import { Sidebar } from "@/components/layout/Sidebar";
import { TopNav } from "@/components/layout/TopNav";

const navItems = [
  { href: "/student/dashboard", label: "Dashboard", icon: IconLayoutDashboard },
  { href: "/student/courses", label: "Courses", icon: IconBook2 },
];

export default function StudentLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen">
      <Sidebar items={navItems} />
      <div className="flex flex-1 flex-col">
        <TopNav />
        <main className="flex-1 p-8">{children}</main>
      </div>
    </div>
  );
}
