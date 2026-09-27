import Link from "next/link";
import { IconChevronRight } from "@tabler/icons-react";

export interface Crumb {
  label: string;
  href?: string;
}

/** Trail back up the hierarchy for pages reached by ID (assignment, quiz, submission, thread, ...). */
export function Breadcrumbs({ items }: { items: Crumb[] }) {
  return (
    <nav aria-label="Breadcrumb" className="mb-4">
      <ol className="flex flex-wrap items-center gap-1 text-sm text-muted-foreground">
        {items.map((item, index) => {
          const last = index === items.length - 1;
          return (
            <li key={`${item.label}-${index}`} className="flex min-w-0 items-center gap-1">
              {item.href && !last ? (
                <Link href={item.href} className="truncate transition-colors hover:text-foreground">
                  {item.label}
                </Link>
              ) : (
                <span className={last ? "truncate font-medium text-foreground" : "truncate"} aria-current={last ? "page" : undefined}>
                  {item.label}
                </span>
              )}
              {!last ? <IconChevronRight className="size-3.5 shrink-0" stroke={1.75} /> : null}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
