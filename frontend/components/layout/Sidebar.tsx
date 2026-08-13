"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { IconBook2, IconLayoutDashboard, IconUsers } from "@tabler/icons-react";

import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: IconLayoutDashboard },
  { href: "/students", label: "Students", icon: IconUsers },
  { href: "/courses", label: "Courses", icon: IconBook2 },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden w-60 shrink-0 flex-col border-r-hairline border-sidebar-border bg-sidebar md:flex">
      <div className="flex h-14 items-center border-b-hairline border-sidebar-border px-6">
        <span className="text-lg font-medium text-sidebar-foreground">Notare</span>
      </div>
      <nav className="flex flex-1 flex-col gap-1 p-3">
        {navItems.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-sidebar-accent text-sidebar-accent-foreground"
                  : "text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground",
              )}
            >
              <Icon className="size-4" stroke={1.75} />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
