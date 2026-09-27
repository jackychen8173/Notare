"use client";

import { useState, type ReactNode } from "react";

import {
  BrandMark,
  Sidebar,
  SidebarContent,
  type SidebarCourses,
  type SidebarNavItem,
} from "@/components/layout/Sidebar";
import { TopNav } from "@/components/layout/TopNav";
import { Sheet, SheetContent, SheetTitle } from "@/components/ui/sheet";

interface AppShellProps {
  navItems: SidebarNavItem[];
  courses?: SidebarCourses;
  profileHref: string;
  children: ReactNode;
}

/**
 * Page chrome for every signed-in area (tutor, student, admin): a sidebar rail on desktop, and the
 * same sidebar in a slide-out drawer on phones, where the rail is hidden.
 */
export function AppShell({ navItems, courses, profileHref, children }: AppShellProps) {
  const [menuOpen, setMenuOpen] = useState(false);

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
        <TopNav profileHref={profileHref} onOpenMenu={() => setMenuOpen(true)} />
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-8">{children}</main>
      </div>
    </div>
  );
}
