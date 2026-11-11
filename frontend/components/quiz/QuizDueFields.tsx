import type { UseFormRegisterReturn } from "react-hook-form";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { wallClockParts, zoneLabel } from "@/lib/dates";

/** "2026-11-20T23:59" for the API from separate date and time inputs; null without a date. */
export function quizDueAt(date: string | undefined, time: string | undefined): string | null {
  return date ? `${date}T${time || "23:59"}` : null;
}

/** Form defaults from a quiz's dueAt (wall clock in the course's zone). */
export function quizDueDefaults(dueAt: string | null): { dueDate: string; dueTime: string } {
  if (!dueAt) return { dueDate: "", dueTime: "" };
  const { date, time } = wallClockParts(dueAt);
  return { dueDate: date, dueTime: time };
}

/** Optional due date and time for a quiz, in the course's time zone. */
export function QuizDueFields({
  idPrefix,
  timeZone,
  dateProps,
  timeProps,
}: {
  idPrefix: string;
  timeZone: string;
  dateProps: UseFormRegisterReturn;
  timeProps: UseFormRegisterReturn;
}) {
  return (
    <fieldset className="flex flex-col gap-1.5">
      <legend className="mb-1.5 text-sm font-medium text-foreground">Due (optional)</legend>
      <div className="grid grid-cols-[minmax(0,1fr)_8rem] gap-2">
        <Input id={`${idPrefix}-due-date`} type="date" aria-label="Quiz due date" {...dateProps} />
        <Input id={`${idPrefix}-due-time`} type="time" aria-label="Quiz due time" placeholder="23:59" {...timeProps} />
      </div>
      <Label htmlFor={`${idPrefix}-due-date`} className="text-xs font-normal text-muted-foreground">
        In {zoneLabel(timeZone)}. No time means 11:59 PM. It shows on calendars and flags the quiz as missing in the
        gradebook; students can still take it after.
      </Label>
    </fieldset>
  );
}
