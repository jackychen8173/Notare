"use client";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { shortDay, type ScheduleFormValues } from "@/lib/schedule";
import { cn } from "@/lib/utils";
import { WEEKDAYS, type Weekday } from "@/types/course";

/** Weekly meeting days/times plus optional term dates, for the new/edit course dialogs. */
export function CourseScheduleFields({
  idPrefix,
  value,
  onChange,
  error,
}: {
  idPrefix: string;
  value: ScheduleFormValues;
  onChange: (value: ScheduleFormValues) => void;
  error?: string;
}) {
  function toggleDay(day: Weekday) {
    const days = value.days.includes(day) ? value.days.filter((d) => d !== day) : [...value.days, day];
    onChange({ ...value, days: WEEKDAYS.filter((d) => days.includes(d)) });
  }

  return (
    <fieldset className="flex flex-col gap-3">
      <legend className="mb-1 text-sm font-medium">
        Class meetings <span className="font-normal text-muted-foreground">(optional)</span>
      </legend>
      <div className="flex flex-wrap gap-1.5" role="group" aria-label="Meeting days">
        {WEEKDAYS.map((day) => {
          const selected = value.days.includes(day);
          return (
            <button
              key={day}
              type="button"
              aria-pressed={selected}
              onClick={() => toggleDay(day)}
              className={cn(
                "h-8 min-w-11 rounded-full border px-3 text-xs font-medium transition-colors outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
                selected
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card text-muted-foreground hover:text-foreground",
              )}
            >
              {shortDay(day)}
            </button>
          );
        })}
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`${idPrefix}-start-time`}>Starts</Label>
          <Input
            id={`${idPrefix}-start-time`}
            type="time"
            value={value.startTime}
            onChange={(e) => onChange({ ...value, startTime: e.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`${idPrefix}-end-time`}>Ends</Label>
          <Input
            id={`${idPrefix}-end-time`}
            type="time"
            value={value.endTime}
            onChange={(e) => onChange({ ...value, endTime: e.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`${idPrefix}-term-start`}>First day of term</Label>
          <Input
            id={`${idPrefix}-term-start`}
            type="date"
            value={value.termStart}
            onChange={(e) => onChange({ ...value, termStart: e.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`${idPrefix}-term-end`}>Last day of term</Label>
          <Input
            id={`${idPrefix}-term-end`}
            type="date"
            value={value.termEnd}
            onChange={(e) => onChange({ ...value, termEnd: e.target.value })}
          />
        </div>
      </div>
      <p className="text-xs text-muted-foreground">
        Meetings repeat weekly on the calendar, between the term dates if you set them.
      </p>
      {error ? <p className="text-xs text-destructive">{error}</p> : null}
    </fieldset>
  );
}
