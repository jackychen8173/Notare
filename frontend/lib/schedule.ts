import { z } from "zod";

import { WEEKDAYS, type CourseSchedule, type Weekday } from "@/types/course";

const SHORT_DAY: Record<Weekday, string> = {
  MONDAY: "Mon",
  TUESDAY: "Tue",
  WEDNESDAY: "Wed",
  THURSDAY: "Thu",
  FRIDAY: "Fri",
  SATURDAY: "Sat",
  SUNDAY: "Sun",
};

export function shortDay(day: Weekday): string {
  return SHORT_DAY[day];
}

/** FullCalendar / Date#getDay numbering: Sunday = 0 ... Saturday = 6. */
export function weekdayIndex(day: Weekday): number {
  return (WEEKDAYS.indexOf(day) + 1) % 7;
}

/** "HH:mm" from the backend's "HH:mm" or "HH:mm:ss". */
export function trimTime(value: string): string {
  return value.slice(0, 5);
}

export function formatTime(value: string): string {
  const [hours, minutes] = trimTime(value).split(":").map(Number);
  return new Date(2000, 0, 1, hours, minutes).toLocaleTimeString(undefined, {
    hour: "numeric",
    minute: "2-digit",
  });
}

/** "Mon, Wed, Fri · 9:00 AM – 9:50 AM", or null when the course has no weekly meeting. */
export function formatMeetingTimes(schedule: CourseSchedule | null): string | null {
  if (!schedule || schedule.days.length === 0 || !schedule.startTime || !schedule.endTime) return null;
  const days = WEEKDAYS.filter((day) => schedule.days.includes(day)).map(shortDay).join(", ");
  return `${days} · ${formatTime(schedule.startTime)} – ${formatTime(schedule.endTime)}`;
}

/** Form-side shape: every field is a string ("" = unset) so it binds straight to inputs. */
export const scheduleFormSchema = z
  .object({
    days: z.array(z.enum(WEEKDAYS as [Weekday, ...Weekday[]])),
    startTime: z.string(),
    endTime: z.string(),
    termStart: z.string(),
    termEnd: z.string(),
  })
  .superRefine((value, ctx) => {
    if (value.days.length > 0) {
      if (!value.startTime || !value.endTime) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Add a start and end time for the meeting days" });
      } else if (value.endTime <= value.startTime) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "End time must be after start time" });
      }
    } else if (value.startTime || value.endTime) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Pick at least one meeting day" });
    }
    if (value.termStart && value.termEnd && value.termEnd < value.termStart) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Term end must be on or after term start" });
    }
  });

export type ScheduleFormValues = z.infer<typeof scheduleFormSchema>;

export function scheduleToForm(schedule: CourseSchedule | null): ScheduleFormValues {
  return {
    days: schedule?.days ?? [],
    startTime: schedule?.startTime ? trimTime(schedule.startTime) : "",
    endTime: schedule?.endTime ? trimTime(schedule.endTime) : "",
    termStart: schedule?.termStart ?? "",
    termEnd: schedule?.termEnd ?? "",
  };
}

export function formToSchedule(values: ScheduleFormValues): CourseSchedule {
  return {
    days: values.days,
    startTime: values.startTime || null,
    endTime: values.endTime || null,
    termStart: values.termStart || null,
    termEnd: values.termEnd || null,
  };
}

/** Whether the course's weekly meeting falls on this date (within the term, when term dates are set). */
export function meetsOn(schedule: CourseSchedule | null, date: Date): boolean {
  if (!schedule || schedule.days.length === 0 || !schedule.startTime) return false;
  if (!schedule.days.some((day) => weekdayIndex(day) === date.getDay())) return false;
  const iso = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  if (schedule.termStart && iso < schedule.termStart) return false;
  if (schedule.termEnd && iso > schedule.termEnd) return false;
  return true;
}
