"use client";

import { useState } from "react";
import { IconArrowBackUp, IconCheck, IconEyeOff, IconSparkles } from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const CODE: { text: string; flagged?: boolean }[] = [
  { text: "public class TemperatureConverter {" },
  { text: "    public static void main(String[] args) {" },
  { text: "        int f = 98;" },
  { text: "        int c = (f - 32) * 5 / 9;", flagged: true },
  { text: '        System.out.println(f + "F is " + c + "C");' },
  { text: "    }" },
  { text: "}" },
];

/**
 * The landing page's hero: the review step that defines Notare. A student's submission, Sage's
 * private draft, and the teacher's release button, which visitors can press to see what the
 * student receives. Static sample content; nothing here calls the backend.
 */
export function ApprovalPreview() {
  const [released, setReleased] = useState(false);

  return (
    <div className="relative flex min-w-0 flex-col gap-3">
      <div className="overflow-hidden rounded-card border border-border bg-card shadow-elevated">
        <div className="flex items-center justify-between gap-3 border-b border-border bg-muted/50 px-4 py-2.5">
          <span className="font-mono text-xs text-foreground">TemperatureConverter.java</span>
          <span className="text-xs text-muted-foreground">Submitted by Sam Patel</span>
        </div>
        <pre className="overflow-x-auto py-3 font-mono text-[12.5px] leading-6">
          {CODE.map((line, index) => (
            <div
              key={index}
              className={cn(
                "flex gap-4 pr-4 pl-3",
                line.flagged && "bg-sage-surface shadow-[inset_3px_0_0_var(--sage)]",
              )}
            >
              <span className="w-4 shrink-0 text-right text-muted-foreground/70 select-none">{index + 1}</span>
              <code className="text-foreground">{line.text}</code>
            </div>
          ))}
        </pre>
      </div>

      <div className="rounded-card border border-sage-border bg-sage-surface p-4 sm:ml-10">
        <div className="mb-1.5 flex items-center gap-1.5 text-xs font-medium text-sage-text">
          <IconSparkles className="size-3.5" stroke={2} aria-hidden />
          Sage&apos;s draft for line 4
        </div>
        <p className="text-sm leading-relaxed text-sage-text">
          Both sides of the division are ints, so 98°F comes out as 36 instead of 36.7. Divide by 9.0 to keep the
          decimal.
        </p>
      </div>

      <div className="flex flex-col gap-3 rounded-card border border-border bg-card p-4 shadow-card sm:ml-10">
        <p className="text-sm text-foreground">
          <span className="text-muted-foreground">Your note: </span>
          So close, Sam. It&apos;s a one-character fix.
        </p>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p
            className={cn(
              "flex items-center gap-1.5 text-xs transition-colors duration-300",
              released ? "text-primary" : "text-muted-foreground",
            )}
            aria-live="polite"
          >
            {released ? (
              <>
                <IconCheck className="size-3.5" stroke={2.25} aria-hidden />
                Sam can see your note and Sage&apos;s feedback now.
              </>
            ) : (
              <>
                <IconEyeOff className="size-3.5" stroke={1.75} aria-hidden />
                Sam can&apos;t see any of this until you release it.
              </>
            )}
          </p>
          {released ? (
            <Button variant="ghost" size="sm" onClick={() => setReleased(false)}>
              <IconArrowBackUp stroke={1.75} />
              Undo
            </Button>
          ) : (
            <Button size="sm" onClick={() => setReleased(true)}>
              Release to Sam
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
