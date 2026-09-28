"use client";

import { IconBackpack, IconChalkboard, IconLoader2 } from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import { useStartDemo, type DemoRole } from "@/hooks/useStartDemo";
import { errorMessage } from "@/lib/api";
import { cn } from "@/lib/utils";

/** "Try it as a teacher / student": one click into a private, pre-filled demo classroom. */
export function DemoButtons({ className }: { className?: string }) {
  const startDemo = useStartDemo();
  const pendingRole = startDemo.isPending ? startDemo.variables : null;

  function button(role: DemoRole, label: string, Icon: typeof IconChalkboard, variant: "default" | "outline") {
    const pending = pendingRole === role;
    return (
      <Button
        size="lg"
        variant={variant}
        className="h-11 px-5 text-[15px]"
        disabled={startDemo.isPending}
        onClick={() => startDemo.mutate(role)}
      >
        {pending ? (
          <IconLoader2 className="size-[18px] animate-spin motion-reduce:animate-none" stroke={2} />
        ) : (
          <Icon className="size-[18px]" stroke={1.75} />
        )}
        {pending ? "Setting up your classroom..." : label}
      </Button>
    );
  }

  return (
    <div className={cn("flex flex-col gap-3", className)}>
      <div className="flex flex-wrap gap-3">
        {button("TUTOR", "Try it as a teacher", IconChalkboard, "default")}
        {button("STUDENT", "Try it as a student", IconBackpack, "outline")}
      </div>
      {startDemo.isError ? (
        <p className="text-sm text-destructive">{errorMessage(startDemo.error)}</p>
      ) : (
        <p className="text-sm text-muted-foreground">No sign-up. You get your own sample class to click around in.</p>
      )}
    </div>
  );
}
