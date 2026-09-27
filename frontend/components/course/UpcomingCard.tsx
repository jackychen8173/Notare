import Link from "next/link";

import { Card, CardContent } from "@/components/ui/card";
import { daysUntil, relativeDueLabel } from "@/lib/dates";
import type { Assignment } from "@/types/assignment";

/** "Upcoming" list for a course's Stream tab: assignments due in the next two weeks. */
export function UpcomingCard({
  assignments,
  href,
}: {
  assignments: Assignment[];
  href: (assignment: Assignment) => string;
}) {
  const upcoming = assignments
    .filter((assignment) => {
      const days = daysUntil(assignment.dueDate);
      return days >= 0 && days <= 14;
    })
    .sort((a, b) => daysUntil(a.dueDate) - daysUntil(b.dueDate))
    .slice(0, 5);

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <p className="text-sm font-semibold text-foreground">Upcoming</p>
        {upcoming.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nothing due in the next two weeks.</p>
        ) : (
          <ul className="flex flex-col gap-1">
            {upcoming.map((assignment) => (
              <li key={assignment.id}>
                <Link
                  href={href(assignment)}
                  className="group -mx-2 flex flex-col rounded-lg px-2 py-1.5 transition-colors hover:bg-muted"
                >
                  <span className="text-sm font-medium text-foreground group-hover:text-primary">
                    {assignment.title}
                  </span>
                  <span className="text-xs text-muted-foreground">{relativeDueLabel(assignment.dueDate)}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
