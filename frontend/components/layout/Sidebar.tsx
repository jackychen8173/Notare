"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ComponentType } from "react";

import { cn } from "@/lib/utils";
import type { CourseColor } from "@/types/course";

export interface SidebarNavItem {
  href: string;
  label: string;
  icon: ComponentType<{ className?: string; stroke?: number }>;
}

export interface SidebarCourseLink {
  id: string;
  name: string;
  color: CourseColor;
}

export interface SidebarCourses {
  // Route prefix for a course's page, e.g. "/courses" or "/student/courses".
  basePath: string;
  items: SidebarCourseLink[] | undefined;
}

interface SidebarProps {
  items: SidebarNavItem[];
  courses?: SidebarCourses;
  onNavigate?: () => void;
}

export function BrandMark() {
  return (
    <span className="flex items-center gap-2">
      <span className="flex size-7 items-center justify-center rounded-lg bg-primary text-sm font-semibold text-primary-foreground">
        N
      </span>
      <span className="text-base font-semibold tracking-tight text-sidebar-foreground">Notare</span>
    </span>
  );
}

function isActive(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`);
}

/** Sidebar body: shared by the desktop rail and the phone drawer. */
export function SidebarContent({ items, courses, onNavigate }: SidebarProps) {
  const pathname = usePathname();
  const courseItems = courses?.items ?? [];

  return (
    <nav className="flex flex-1 flex-col gap-6 overflow-y-auto p-3">
      <div className="flex flex-col gap-0.5">
        {items.map((item) => {
          // A course page lives under the Courses route but is highlighted in the course list below,
          // so the Courses entry only lights up on the list page itself.
          const active =
            courses && item.href === courses.basePath
              ? pathname === item.href
              : isActive(pathname, item.href);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              onClick={onNavigate}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                active
                  ? "bg-primary-soft text-primary"
                  : "text-sidebar-foreground/75 hover:bg-sidebar-accent hover:text-sidebar-foreground",
              )}
            >
              <Icon className="size-[18px]" stroke={1.75} />
              {item.label}
            </Link>
          );
        })}
      </div>

      {courses && courseItems.length > 0 ? (
        <div className="flex flex-col gap-0.5">
          <p className="px-3 pb-1 text-xs font-medium tracking-wide text-muted-foreground uppercase">
            Your courses
          </p>
          {courseItems.map((course) => {
            const href = `${courses.basePath}/${course.id}`;
            const active = isActive(pathname, href);
            return (
              <Link
                key={course.id}
                href={href}
                onClick={onNavigate}
                data-course-color={course.color}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-1.5 text-sm transition-colors",
                  active
                    ? "bg-sidebar-accent font-medium text-sidebar-foreground"
                    : "text-sidebar-foreground/75 hover:bg-sidebar-accent hover:text-sidebar-foreground",
                )}
              >
                <span className="size-2.5 shrink-0 rounded-full bg-course" aria-hidden />
                <span className="truncate">{course.name}</span>
              </Link>
            );
          })}
        </div>
      ) : null}
    </nav>
  );
}

export function Sidebar(props: SidebarProps) {
  return (
    <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col border-r border-sidebar-border bg-sidebar md:flex">
      <div className="flex h-14 shrink-0 items-center px-5">
        <BrandMark />
      </div>
      <SidebarContent {...props} />
    </aside>
  );
}
