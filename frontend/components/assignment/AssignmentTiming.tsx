import { IconCalendarTime, IconClockExclamation, IconLock } from "@tabler/icons-react";

import { formatDueAt, isPast } from "@/lib/dates";
import type { Assignment } from "@/types/assignment";

/** "Late work: 10% off per day late, until Fri, Nov 27, 11:59 PM", and so on. */
export function lateRule(assignment: Assignment): string {
  if (assignment.closesAt != null && assignment.closesAt === assignment.dueAt) {
    return "No late work: it closes at the due time";
  }
  const until = assignment.closesAt ? `, until ${formatDueAt(assignment.closesAt, assignment.timeZone)}` : "";
  if (assignment.latePenaltyPercent > 0) {
    return `Late work: ${assignment.latePenaltyPercent}% off per day late${until}`;
  }
  return `Late work accepted${until}`;
}

/**
 * The due time, points and late rules for an assignment, as one line of facts. `audience`
 * switches the wording of the scheduled-opening note.
 */
export function AssignmentTiming({ assignment, audience }: { assignment: Assignment; audience: "tutor" | "student" }) {
  const closed = !assignment.acceptingSubmissions && assignment.closesAt != null && isPast(assignment.closesAt);
  const scheduled = audience === "tutor" && assignment.opensAt != null && !isPast(assignment.opensAt);

  return (
    <div className="flex flex-col gap-1 text-sm text-muted-foreground">
      <p className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <span className="text-foreground">Due {formatDueAt(assignment.dueAt, assignment.timeZone)}</span>
        {assignment.pointsPossible != null ? (
          <span className="font-mono text-xs">{assignment.pointsPossible} pts</span>
        ) : null}
      </p>
      <p>{lateRule(assignment)}</p>
      {scheduled ? (
        <p className="flex items-center gap-1.5 text-primary">
          <IconCalendarTime className="size-4" />
          Students can&apos;t see this until {formatDueAt(assignment.opensAt!, assignment.timeZone)}
        </p>
      ) : null}
      {closed ? (
        <p className="flex items-center gap-1.5 text-foreground">
          <IconLock className="size-4" />
          Closed: {audience === "tutor" ? "students can't submit any more" : "this no longer accepts submissions"}
        </p>
      ) : null}
    </div>
  );
}

/** "Late · 2 days · −20%" next to a submission. */
export function LateBadge({ daysLate, latePenaltyPercent }: { daysLate: number; latePenaltyPercent: number }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-md bg-warning-surface px-1.5 py-0.5 font-mono text-xs text-warning">
      <IconClockExclamation className="size-3.5" />
      Late · {daysLate} {daysLate === 1 ? "day" : "days"}
      {latePenaltyPercent > 0 ? ` · −${latePenaltyPercent}%` : ""}
    </span>
  );
}
