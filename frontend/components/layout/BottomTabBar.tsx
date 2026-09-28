"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import type { SidebarNavItem } from "@/components/layout/Sidebar";
import { cn } from "@/lib/utils";

/** Phone-only tab bar for the main pages; the drawer (top-left menu) still has the course list. */
export function BottomTabBar({ items }: { items: SidebarNavItem[] }) {
  const pathname = usePathname();

  return (
    <nav
      aria-label="Main"
      className="fixed inset-x-0 bottom-0 z-30 flex border-t border-border bg-background/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden"
    >
      {items.slice(0, 5).map((item) => {
        const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
        const Icon = item.icon;
        return (
          <Link
            key={item.href}
            href={item.href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "flex min-w-0 flex-1 flex-col items-center gap-0.5 py-2 text-[11px] font-medium transition-colors",
              active ? "text-primary" : "text-muted-foreground",
            )}
          >
            <Icon className="size-5" stroke={active ? 2 : 1.75} />
            <span className="truncate">{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
