"use client";

import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";

import { BottomTabBar } from "@/components/layout/BottomTabBar";
import { CommandPalette, type SearchRole } from "@/components/layout/CommandPalette";
import { DemoBanner } from "@/components/layout/DemoBanner";
import {
  BrandMark,
  Sidebar,
  SidebarContent,
  type SidebarCourses,
  type SidebarNavItem,
} from "@/components/layout/Sidebar";
import { ShortcutsDialog } from "@/components/layout/ShortcutsDialog";
import { StatusBar } from "@/components/layout/StatusBar";
import { TopNav } from "@/components/layout/TopNav";
import { Sheet, SheetContent, SheetTitle } from "@/components/ui/sheet";

interface AppShellProps {
  role: SearchRole;
  navItems: SidebarNavItem[];
  courses?: SidebarCourses;
  profileHref: string;
  children: ReactNode;
}

/**
 * Page chrome for every signed-in area (tutor, student, admin): a sidebar rail on desktop; on phones,
 * a bottom tab bar for the main pages plus the full sidebar in a slide-out drawer. Ctrl/Cmd+K opens
 * search from anywhere, "?" lists shortcuts, and G then a nav item's letter jumps to it.
 */

/** True when a keypress should go to a text field rather than a page shortcut. */
function isTyping(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) return false;
  return target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName);
}
export function AppShell({ role, navItems, courses, profileHref, children }: AppShellProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [shortcutsOpen, setShortcutsOpen] = useState(false);
  const router = useRouter();

  useEffect(() => {
    let goPending = false;
    let goTimer: number | undefined;

    function onKeyDown(event: KeyboardEvent) {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setSearchOpen((open) => !open);
        return;
      }
      if (event.metaKey || event.ctrlKey || event.altKey || isTyping(event.target)) return;

      if (goPending) {
        goPending = false;
        window.clearTimeout(goTimer);
        const item = navItems.find((candidate) => candidate.shortcut === event.key.toLowerCase());
        if (item) {
          event.preventDefault();
          router.push(item.href);
        }
        return;
      }
      if (event.key === "?") {
        event.preventDefault();
        setShortcutsOpen((open) => !open);
      } else if (event.key === "g") {
        // "G then H" style: the second key has a second to arrive.
        goPending = true;
        goTimer = window.setTimeout(() => (goPending = false), 1000);
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => {
      window.removeEventListener("keydown", onKeyDown);
      window.clearTimeout(goTimer);
    };
  }, [navItems, router]);

  return (
    <div className="flex min-h-screen">
      <Sidebar items={navItems} courses={courses} />

      <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
        <SheetContent side="left" className="w-72 gap-0 bg-sidebar p-0">
          <div className="flex h-14 shrink-0 items-center px-5">
            <SheetTitle render={<div />}>
              <BrandMark />
            </SheetTitle>
          </div>
          <SidebarContent items={navItems} courses={courses} onNavigate={() => setMenuOpen(false)} />
        </SheetContent>
      </Sheet>

      <div className="flex min-w-0 flex-1 flex-col">
        <DemoBanner />
        <TopNav profileHref={profileHref} onOpenMenu={() => setMenuOpen(true)} onOpenSearch={() => setSearchOpen(true)} />
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 pt-5 pb-24 sm:px-6 md:pb-6 lg:px-8 lg:py-6">{children}</main>
        <StatusBar role={role} onOpenShortcuts={() => setShortcutsOpen(true)} />
      </div>

      <BottomTabBar items={navItems} />
      <ShortcutsDialog open={shortcutsOpen} onOpenChange={setShortcutsOpen} navItems={navItems} />
      <CommandPalette open={searchOpen} onOpenChange={setSearchOpen} role={role} navItems={navItems} courses={courses} />
    </div>
  );
}
