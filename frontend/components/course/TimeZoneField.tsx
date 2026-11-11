"use client";

import { useMemo } from "react";

import { Label } from "@/components/ui/label";
import { browserTimeZone } from "@/lib/dates";

// Canada first (Notare starts in BC), then everything else the browser knows.
const CANADA = [
  "America/Vancouver",
  "America/Edmonton",
  "America/Regina",
  "America/Winnipeg",
  "America/Toronto",
  "America/Halifax",
  "America/St_Johns",
];

function allZones(): string[] {
  try {
    return Intl.supportedValuesOf("timeZone");
  } catch {
    return [];
  }
}

/** The course's time zone. Due times are set and shown in it. */
export function TimeZoneField({
  id,
  value,
  onChange,
  hint,
}: {
  id: string;
  value: string;
  onChange: (value: string) => void;
  hint?: string;
}) {
  const others = useMemo(() => {
    const zones = new Set([browserTimeZone(), value, ...allZones()]);
    CANADA.forEach((zone) => zones.delete(zone));
    return [...zones].filter(Boolean).sort();
  }, [value]);

  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>Time zone</Label>
      <select
        id={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-8 w-full rounded-lg border border-input bg-card px-2.5 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <optgroup label="Canada">
          {CANADA.map((zone) => (
            <option key={zone} value={zone}>
              {zone.replace(/_/g, " ")}
            </option>
          ))}
        </optgroup>
        <optgroup label="Everywhere else">
          {others.map((zone) => (
            <option key={zone} value={zone}>
              {zone.replace(/_/g, " ")}
            </option>
          ))}
        </optgroup>
      </select>
      <p className="text-xs text-muted-foreground">{hint ?? "Due times in this course are set in this time zone."}</p>
    </div>
  );
}
