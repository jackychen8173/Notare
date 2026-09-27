"use client";

import { IconCheck } from "@tabler/icons-react";

import { cn } from "@/lib/utils";
import { COURSE_COLORS, type CourseColor } from "@/types/course";

export function CourseColorPicker({
  value,
  onChange,
}: {
  value: CourseColor | undefined;
  onChange: (color: CourseColor) => void;
}) {
  return (
    <div className="flex flex-wrap gap-2" role="radiogroup" aria-label="Course color">
      {COURSE_COLORS.map((color) => {
        const selected = value === color;
        return (
          <button
            key={color}
            type="button"
            role="radio"
            aria-checked={selected}
            aria-label={color.toLowerCase()}
            data-course-color={color}
            onClick={() => onChange(color)}
            className={cn(
              "flex size-8 items-center justify-center rounded-full bg-course-solid text-white transition-transform outline-none hover:scale-110 focus-visible:ring-3 focus-visible:ring-ring/50",
              selected && "ring-2 ring-foreground ring-offset-2 ring-offset-background",
            )}
          >
            {selected ? <IconCheck className="size-4" stroke={2.5} /> : null}
          </button>
        );
      })}
    </div>
  );
}
