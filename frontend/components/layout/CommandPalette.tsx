"use client";

import { useEffect, useMemo, useRef, useState, type ComponentType } from "react";
import { useRouter } from "next/navigation";
import { useTheme } from "next-themes";
import {
  IconBook2,
  IconCornerDownLeft,
  IconFileCode,
  IconLogout,
  IconMoon,
  IconSearch,
  IconSun,
  IconUser,
} from "@tabler/icons-react";

import type { SidebarCourseLink, SidebarNavItem } from "@/components/layout/Sidebar";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { useAssignmentsForCourses } from "@/hooks/useAssignments";
import { useSignOut } from "@/hooks/useSignOut";
import { useStudents } from "@/hooks/useStudents";
import { cn } from "@/lib/utils";
import type { CourseColor } from "@/types/course";

export type SearchRole = "tutor" | "student" | "admin";

type Icon = ComponentType<{ className?: string; stroke?: number }>;

interface CommandItem {
  id: string;
  group: string;
  label: string;
  hint?: string;
  icon: Icon;
  color?: CourseColor;
  /** Only shown once the user types (e.g. a course's individual tabs), to keep the default list short. */
  searchOnly?: boolean;
  run: () => void;
}

const COURSE_TABS: Record<SearchRole, { id: string; label: string }[]> = {
  tutor: [
    { id: "classwork", label: "Classwork" },
    { id: "progress", label: "Progress" },
    { id: "people", label: "People" },
    { id: "discussions", label: "Discussions" },
    { id: "settings", label: "Settings" },
  ],
  student: [
    { id: "classwork", label: "Classwork" },
    { id: "progress", label: "Progress" },
    { id: "discussions", label: "Discussions" },
  ],
  admin: [],
};

const GROUP_ORDER = ["Go to", "Courses", "Assignments", "Students", "Actions"];

/** Every word typed has to appear somewhere in the label or hint, in any order. */
function matches(item: CommandItem, query: string): boolean {
  const haystack = `${item.label} ${item.hint ?? ""}`.toLowerCase();
  return query
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .every((word) => haystack.includes(word));
}

interface CommandPaletteProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  role: SearchRole;
  navItems: SidebarNavItem[];
  courses?: { basePath: string; items: SidebarCourseLink[] | undefined };
}

/** Ctrl/Cmd+K: jump to any page, course, assignment or student, or run a quick action. */
export function CommandPalette(props: CommandPaletteProps) {
  return (
    <Dialog open={props.open} onOpenChange={props.onOpenChange}>
      <DialogContent showCloseButton={false} className="top-[15%] translate-y-0 gap-0 overflow-hidden p-0 sm:max-w-xl">
        <DialogTitle className="sr-only">Search Notare</DialogTitle>
        {/* Mounted only while open, so its data hooks don't run on every page. */}
        {props.open ? <PaletteBody {...props} /> : null}
      </DialogContent>
    </Dialog>
  );
}

function PaletteBody({ onOpenChange, role, navItems, courses }: CommandPaletteProps) {
  const router = useRouter();
  const signOut = useSignOut();
  const { resolvedTheme, setTheme } = useTheme();
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const listRef = useRef<HTMLDivElement>(null);

  const courseList = useMemo(() => courses?.items ?? [], [courses?.items]);
  const assignments = useAssignmentsForCourses(
    role === "admin" ? [] : courseList.map((course) => course.id),
    role === "student",
  );
  const students = useStudents({ enabled: role === "tutor" });

  const items = useMemo<CommandItem[]>(() => {
    const go = (href: string) => () => {
      onOpenChange(false);
      router.push(href);
    };
    const colorOf = new Map(courseList.map((course) => [course.id, course.color]));
    const list: CommandItem[] = navItems.map((item) => ({
      id: `nav-${item.href}`,
      group: "Go to",
      label: item.label,
      icon: item.icon,
      run: go(item.href),
    }));

    if (courses) {
      for (const course of courseList) {
        const base = `${courses.basePath}/${course.id}`;
        list.push({ id: `course-${course.id}`, group: "Courses", label: course.name, icon: IconBook2, color: course.color, run: go(base) });
        for (const tab of COURSE_TABS[role]) {
          list.push({
            id: `course-${course.id}-${tab.id}`,
            group: "Courses",
            label: `${course.name}: ${tab.label}`,
            icon: IconBook2,
            color: course.color,
            searchOnly: true,
            run: go(`${base}?tab=${tab.id}`),
          });
        }
      }
    }

    for (const assignment of assignments.data) {
      list.push({
        id: `assignment-${assignment.id}`,
        group: "Assignments",
        label: assignment.title,
        hint: assignment.courseName,
        icon: IconFileCode,
        color: colorOf.get(assignment.courseId),
        run: go(role === "student" ? `/student/assignments/${assignment.id}` : `/assignments/${assignment.id}`),
      });
    }

    for (const student of students.data ?? []) {
      list.push({
        id: `student-${student.id}`,
        group: "Students",
        label: student.name,
        hint: student.email,
        icon: IconUser,
        run: go(`/students/${student.id}`),
      });
    }

    const dark = resolvedTheme === "dark";
    list.push(
      {
        id: "theme",
        group: "Actions",
        label: dark ? "Switch to light mode" : "Switch to dark mode",
        icon: dark ? IconSun : IconMoon,
        run: () => {
          setTheme(dark ? "light" : "dark");
          onOpenChange(false);
        },
      },
      { id: "sign-out", group: "Actions", label: "Sign out", icon: IconLogout, run: () => signOut() },
    );
    return list;
  }, [navItems, courses, courseList, assignments.data, students.data, role, resolvedTheme, setTheme, signOut, router, onOpenChange]);

  // With no query, show pages, courses (not every tab) and actions; typing searches everything.
  const visible = useMemo(() => {
    const filtered = query.trim()
      ? items.filter((item) => matches(item, query))
      : items.filter((item) => !item.searchOnly && ["Go to", "Courses", "Actions"].includes(item.group));
    return GROUP_ORDER.flatMap((group) => filtered.filter((item) => item.group === group).slice(0, 8));
  }, [items, query]);

  const activeIndex = Math.min(active, Math.max(visible.length - 1, 0));

  useEffect(() => {
    listRef.current?.querySelector(`[data-index="${activeIndex}"]`)?.scrollIntoView({ block: "nearest" });
  }, [activeIndex]);

  function onKeyDown(event: React.KeyboardEvent) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActive((activeIndex + 1) % Math.max(visible.length, 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActive((activeIndex - 1 + visible.length) % Math.max(visible.length, 1));
    } else if (event.key === "Enter") {
      event.preventDefault();
      visible[activeIndex]?.run();
    }
  }

  return (
    <div className="flex flex-col">
      <div className="flex items-center gap-2 border-b border-border px-4">
        <IconSearch className="size-4 shrink-0 text-muted-foreground" stroke={1.75} aria-hidden />
        <input
          autoFocus
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            setActive(0);
          }}
          onKeyDown={onKeyDown}
          placeholder={role === "tutor" ? "Search courses, assignments, students..." : "Search pages, courses, assignments..."}
          className="h-12 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
          role="combobox"
          aria-expanded
          aria-controls="command-results"
          aria-activedescendant={visible[activeIndex] ? `command-${visible[activeIndex].id}` : undefined}
        />
        <kbd className="hidden rounded border border-border px-1.5 py-0.5 text-[10px] text-muted-foreground sm:inline">Esc</kbd>
      </div>
      <div ref={listRef} id="command-results" role="listbox" className="max-h-[min(60vh,420px)] overflow-y-auto p-2">
        {visible.length === 0 ? (
          <p className="px-3 py-8 text-center text-sm text-muted-foreground">No matches for &ldquo;{query}&rdquo;.</p>
        ) : (
          visible.map((item, index) => {
            const showGroup = index === 0 || visible[index - 1].group !== item.group;
            const Icon = item.icon;
            const selected = index === activeIndex;
            return (
              <div key={item.id}>
                {showGroup ? <p className="px-3 pt-2 pb-1 text-xs font-medium text-muted-foreground">{item.group}</p> : null}
                <div
                  id={`command-${item.id}`}
                  role="option"
                  aria-selected={selected}
                  data-index={index}
                  data-course-color={item.color}
                  onMouseMove={() => setActive(index)}
                  onClick={() => item.run()}
                  className={cn(
                    "flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-sm",
                    selected ? "bg-muted text-foreground" : "text-foreground/85",
                  )}
                >
                  <Icon className={cn("size-4 shrink-0", item.color ? "text-course" : "text-muted-foreground")} stroke={1.75} />
                  <span className="min-w-0 flex-1 truncate">{item.label}</span>
                  {item.hint ? <span className="hidden max-w-[40%] truncate text-xs text-muted-foreground sm:inline">{item.hint}</span> : null}
                  {selected ? <IconCornerDownLeft className="size-3.5 shrink-0 text-muted-foreground" stroke={1.75} aria-hidden /> : null}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
