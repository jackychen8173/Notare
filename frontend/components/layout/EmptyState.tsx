import type { ComponentType, ReactNode } from "react";

import { cn } from "@/lib/utils";

interface EmptyStateProps {
  icon: ComponentType<{ className?: string; stroke?: number }>;
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

export function EmptyState({ icon: Icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-2 rounded-card border border-dashed border-border px-6 py-10 text-center",
        className,
      )}
    >
      <div className="mb-1 flex size-10 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <Icon className="size-5" stroke={1.75} />
      </div>
      <p className="text-sm font-medium text-foreground">{title}</p>
      {description ? <p className="max-w-sm text-sm text-muted-foreground">{description}</p> : null}
      {action ? <div className="mt-2">{action}</div> : null}
    </div>
  );
}
