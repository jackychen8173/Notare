"use client";

import { IconMenu2, IconSearch } from "@tabler/icons-react";

import { AccountMenu } from "@/components/layout/AccountMenu";
import { ThemeToggle } from "@/components/layout/ThemeToggle";
import { BrandMark } from "@/components/layout/Sidebar";
import { Button } from "@/components/ui/button";

interface TopNavProps {
  profileHref: string;
  onOpenMenu: () => void;
  onOpenSearch: () => void;
}

export function TopNav({ profileHref, onOpenMenu, onOpenSearch }: TopNavProps) {
  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-2 border-b border-border bg-background/85 px-4 backdrop-blur sm:px-6">
      <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open menu" onClick={onOpenMenu}>
        <IconMenu2 className="size-5" stroke={1.75} />
      </Button>
      <div className="md:hidden">
        <BrandMark />
      </div>
      <button
        type="button"
        onClick={onOpenSearch}
        className="hidden h-9 w-72 items-center gap-2 rounded-lg border border-border bg-card px-3 text-sm text-muted-foreground shadow-xs transition-colors outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 md:flex"
      >
        <IconSearch className="size-4" stroke={1.75} aria-hidden />
        <span className="flex-1 text-left">Search or jump to...</span>
        <kbd className="rounded border border-border px-1.5 py-0.5 font-sans text-[10px]">Ctrl K</kbd>
      </button>
      <div className="ml-auto flex items-center gap-1">
        <Button variant="ghost" size="icon" className="md:hidden" aria-label="Search" onClick={onOpenSearch}>
          <IconSearch className="size-5" stroke={1.75} />
        </Button>
        <ThemeToggle />
        <AccountMenu profileHref={profileHref} />
      </div>
    </header>
  );
}
