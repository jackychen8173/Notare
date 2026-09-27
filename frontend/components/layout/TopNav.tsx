"use client";

import Link from "next/link";
import { IconMenu2, IconUserCircle } from "@tabler/icons-react";

import { ThemeToggle } from "@/components/layout/ThemeToggle";
import { BrandMark } from "@/components/layout/Sidebar";
import { Button } from "@/components/ui/button";

interface TopNavProps {
  profileHref: string;
  onOpenMenu: () => void;
}

export function TopNav({ profileHref, onOpenMenu }: TopNavProps) {
  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-2 border-b border-border bg-background/85 px-4 backdrop-blur sm:px-6">
      <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open menu" onClick={onOpenMenu}>
        <IconMenu2 className="size-5" stroke={1.75} />
      </Button>
      <div className="md:hidden">
        <BrandMark />
      </div>
      <div className="ml-auto flex items-center gap-1">
        <ThemeToggle />
        <Link
          href={profileHref}
          className="flex size-8 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          aria-label="Account"
        >
          <IconUserCircle className="size-5" stroke={1.75} />
        </Link>
      </div>
    </header>
  );
}
