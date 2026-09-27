import Link from "next/link";
import type { ComponentType } from "react";

import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface StatTileProps {
  label: string;
  value: number | undefined;
  isLoading?: boolean;
  icon: ComponentType<{ className?: string; stroke?: number }>;
  href?: string;
  /** "sage" is only for AI-related counts (Sage drafts awaiting review). */
  tone?: "default" | "sage";
}

export function StatTile({ label, value, isLoading, icon: Icon, href, tone = "default" }: StatTileProps) {
  const body = (
    <div
      className={cn(
        "flex items-center gap-4 rounded-card border border-border bg-card p-3 shadow-card sm:p-4",
        href && "transition-all hover:-translate-y-0.5 hover:shadow-elevated",
      )}
    >
      <div
        className={cn(
          "hidden size-10 shrink-0 items-center justify-center rounded-lg sm:flex",
          tone === "sage" ? "bg-sage-surface text-sage" : "bg-primary-soft text-primary",
        )}
      >
        <Icon className="size-5" stroke={1.75} />
      </div>
      <div className="min-w-0">
        <p className="text-xs text-muted-foreground sm:text-sm">{label}</p>
        {isLoading ? (
          <Skeleton className="mt-1 h-7 w-10" />
        ) : (
          <p className="text-2xl font-semibold tracking-tight text-foreground">{value ?? 0}</p>
        )}
      </div>
    </div>
  );

  return href ? <Link href={href}>{body}</Link> : body;
}
