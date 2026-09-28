import { IconCircleCheckFilled, IconPin, IconPlayerPlay } from "@tabler/icons-react";

import { cn } from "@/lib/utils";

/*
 * Small, static recreations of real Notare screens for the landing page. They use the app's own
 * tokens (course colors, sage) so they stay true to the product in light and dark mode.
 */

const WEEK: { day: string; items: { label: string; color: string; kind: "meeting" | "due" | "session" }[] }[] = [
  { day: "Mon", items: [{ label: "P3 9:00", color: "INDIGO", kind: "meeting" }] },
  {
    day: "Tue",
    items: [
      { label: "Due: Strings", color: "INDIGO", kind: "due" },
      { label: "P5 1:00", color: "ROSE", kind: "meeting" },
    ],
  },
  {
    day: "Wed",
    items: [
      { label: "P3 9:00", color: "INDIGO", kind: "meeting" },
      { label: "4:00 1:1", color: "INDIGO", kind: "session" },
    ],
  },
  { day: "Thu", items: [{ label: "P5 1:00", color: "ROSE", kind: "meeting" }] },
  {
    day: "Fri",
    items: [
      { label: "P3 9:00", color: "INDIGO", kind: "meeting" },
      { label: "Due: Grades", color: "ROSE", kind: "due" },
    ],
  },
];

export function CalendarFragment() {
  return (
    <div className="grid grid-cols-5 gap-1.5 rounded-card border border-border bg-card p-3 shadow-card">
      {WEEK.map((column) => (
        <div key={column.day} className="flex min-w-0 flex-col gap-1.5">
          <span className="px-1 text-xs font-medium text-muted-foreground">{column.day}</span>
          {column.items.map((item) => (
            <span
              key={item.label}
              data-course-color={item.color}
              className={cn(
                "truncate rounded-md px-1.5 py-1 text-[11px] font-medium",
                item.kind === "meeting" && "bg-course-soft text-foreground shadow-[inset_2px_0_0_var(--course)]",
                item.kind === "due" && "bg-course-solid text-white",
                item.kind === "session" && "border border-dashed border-course text-foreground",
              )}
            >
              {item.label}
            </span>
          ))}
        </div>
      ))}
    </div>
  );
}

export function RunFragment() {
  return (
    <div className="overflow-hidden rounded-card border border-border bg-card shadow-card">
      <pre className="overflow-x-auto px-4 py-3 font-mono text-[12.5px] leading-6 text-foreground">
        <code>{`double f = 98.6;
double c = (f - 32) * 5.0 / 9;
System.out.printf("%.1f°C%n", c);`}</code>
      </pre>
      <div className="flex items-center justify-between gap-3 border-t border-border bg-muted/50 px-4 py-2.5">
        <span className="inline-flex items-center gap-1.5 rounded-md bg-primary px-2.5 py-1 text-xs font-medium text-primary-foreground">
          <IconPlayerPlay className="size-3" stroke={2.5} aria-hidden />
          Run
        </span>
        <span className="font-mono text-xs text-foreground">37.0°C</span>
      </div>
    </div>
  );
}

export function QuizFragment() {
  const options = ["3.5", "3", "4", "3.0"];
  return (
    <div className="flex flex-col gap-3 rounded-card border border-border bg-card p-4 shadow-card">
      <p className="text-sm text-foreground">
        What does <code className="font-mono text-[13px]">System.out.println(7 / 2);</code> print?
      </p>
      <div className="grid grid-cols-2 gap-2">
        {options.map((option) => {
          const correct = option === "3";
          return (
            <span
              key={option}
              className={cn(
                "flex items-center justify-between rounded-lg border px-3 py-2 font-mono text-sm",
                correct ? "border-primary bg-primary-soft text-foreground" : "border-border text-muted-foreground",
              )}
            >
              {option}
              {correct ? <IconCircleCheckFilled className="size-4 text-primary" aria-label="Correct" /> : null}
            </span>
          );
        })}
      </div>
      <p className="text-xs text-muted-foreground">Scored automatically: 1 / 1 point</p>
    </div>
  );
}

export function DiscussionFragment() {
  return (
    <div className="flex flex-col gap-3 rounded-card border border-border bg-card p-4 shadow-card">
      <div className="flex flex-col gap-1">
        <p className="text-sm font-medium text-foreground">Why does 7 / 2 give 3?</p>
        <p className="text-xs text-muted-foreground">Anonymous to classmates, 1 reply</p>
      </div>
      <p className="text-sm text-foreground">I expected 3.5. Is my computer broken?</p>
      <div className="flex flex-col gap-1 rounded-lg bg-muted/60 p-3">
        <p className="text-xs font-medium text-foreground">Alex Rivera, teacher</p>
        <p className="text-sm text-foreground">
          When both numbers are ints, Java drops the remainder. Try 7 / 2.0.
        </p>
      </div>
      <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <IconPin className="size-3.5" stroke={1.75} aria-hidden />
        Pinned by your teacher
      </p>
    </div>
  );
}
