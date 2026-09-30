"use client";

import { usePathname } from "next/navigation";
import { useTheme } from "next-themes";

import type { SearchRole } from "@/components/layout/CommandPalette";
import { Kbd } from "@/components/ui/kbd";

const ROLE_LABEL: Record<SearchRole, string> = { tutor: "teacher", student: "student", admin: "admin" };

// IDs in the URL are UUIDs; show just enough of one to tell pages apart.
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Editor-style status line along the bottom of the page (desktop only). */
export function StatusBar({ role, onOpenShortcuts }: { role: SearchRole; onOpenShortcuts: () => void }) {
  const pathname = usePathname();
  const { resolvedTheme } = useTheme();
  const path = pathname
    .split("/")
    .filter(Boolean)
    .map((segment) => (UUID.test(segment) ? segment.slice(0, 8) : segment))
    .join("/");

  return (
    <footer className="sticky bottom-0 z-20 hidden h-7 shrink-0 items-center gap-4 border-t border-border bg-sidebar px-4 font-mono text-[11px] text-muted-foreground md:flex">
      <span className="flex items-center gap-1.5">
        <span className="size-1.5 rounded-full bg-primary" aria-hidden />
        {ROLE_LABEL[role]}
      </span>
      <span className="min-w-0 truncate">~/{path}</span>
      <span className="ml-auto flex items-center gap-4">
        <span className="hidden lg:inline">{resolvedTheme === "dark" ? "dark" : "light"}</span>
        <span className="flex items-center gap-1">
          <Kbd>Ctrl</Kbd>
          <Kbd>K</Kbd> search
        </span>
        <button type="button" onClick={onOpenShortcuts} className="flex items-center gap-1 hover:text-foreground">
          <Kbd>?</Kbd> shortcuts
        </button>
      </span>
    </footer>
  );
}
