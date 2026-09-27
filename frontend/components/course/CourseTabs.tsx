"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";

import { cn } from "@/lib/utils";

export interface CourseTab {
  id: string;
  label: string;
  badge?: number;
}

/** Reads the active tab from `?tab=`, falling back to the first tab. */
export function useCourseTab(tabs: CourseTab[]): string {
  const searchParams = useSearchParams();
  const requested = searchParams.get("tab");
  return tabs.some((tab) => tab.id === requested) ? (requested as string) : tabs[0].id;
}

/** Tab bar for a course page. Each tab is a real URL (`?tab=...`) so back/forward and bookmarks work. */
export function CourseTabs({ tabs, active }: { tabs: CourseTab[]; active: string }) {
  const pathname = usePathname();

  return (
    <div className="-mx-4 overflow-x-auto px-4 sm:mx-0 sm:px-0">
      <nav className="flex min-w-max gap-1 border-b border-border" aria-label="Course sections">
        {tabs.map((tab, index) => {
          const selected = tab.id === active;
          return (
            <Link
              key={tab.id}
              href={index === 0 ? pathname : `${pathname}?tab=${tab.id}`}
              scroll={false}
              aria-current={selected ? "page" : undefined}
              className={cn(
                "relative -mb-px flex items-center gap-2 border-b-2 px-3 py-2.5 text-sm font-medium transition-colors",
                selected
                  ? "border-primary text-foreground"
                  : "border-transparent text-muted-foreground hover:text-foreground",
              )}
            >
              {tab.label}
              {tab.badge ? (
                <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1.5 text-[11px] font-semibold text-primary-foreground">
                  {tab.badge}
                </span>
              ) : null}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
