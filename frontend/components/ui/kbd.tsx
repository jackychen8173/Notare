import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

/** A keyboard key, e.g. <Kbd>Ctrl</Kbd>. Pass several children to show a combo. */
export function Kbd({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <kbd
      className={cn(
        "inline-flex h-5 min-w-5 items-center justify-center rounded border border-border bg-muted px-1 font-mono text-[10px] font-medium text-muted-foreground",
        className,
      )}
    >
      {children}
    </kbd>
  );
}
