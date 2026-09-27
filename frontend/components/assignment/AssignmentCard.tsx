import Link from "next/link";
import { IconFileText } from "@tabler/icons-react";

import { Badge } from "@/components/ui/badge";
import { daysUntil, formatDueDate } from "@/lib/dates";
import type { Assignment } from "@/types/assignment";

interface AssignmentCardProps {
  assignment: Assignment;
  /** Defaults to the tutor detail route; pass the student route explicitly there. */
  href?: string;
}

export function AssignmentCard({ assignment, href }: AssignmentCardProps) {
  const days = daysUntil(assignment.dueDate);

  return (
    <Link
      href={href ?? `/assignments/${assignment.id}`}
      className="group flex items-center gap-3 rounded-card border border-border bg-card p-3 shadow-card transition-all hover:-translate-y-px hover:shadow-elevated"
    >
      <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary-soft text-primary">
        <IconFileText className="size-[18px]" stroke={1.75} />
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-foreground">{assignment.title}</p>
        <p className="text-xs text-muted-foreground">Due {formatDueDate(assignment.dueDate)}</p>
      </div>
      {days >= 0 && days <= 2 ? (
        <Badge className="bg-warning-surface text-warning">{days === 0 ? "Due today" : "Due soon"}</Badge>
      ) : null}
    </Link>
  );
}
