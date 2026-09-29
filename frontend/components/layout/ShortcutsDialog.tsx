"use client";

import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Kbd } from "@/components/ui/kbd";
import type { SidebarNavItem } from "@/components/layout/Sidebar";

interface ShortcutsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  navItems: SidebarNavItem[];
}

const GENERAL: { keys: string[]; label: string }[] = [
  { keys: ["Ctrl", "K"], label: "Search and jump anywhere" },
  { keys: ["?"], label: "Show this list" },
  { keys: ["Esc"], label: "Close a dialog or leave full screen" },
];

const CODE: { keys: string[]; label: string }[] = [
  { keys: ["Ctrl", "Enter"], label: "Run code (editor), or save a line comment" },
  { keys: ["J"], label: "Next submission in the review queue" },
  { keys: ["K"], label: "Previous submission in the review queue" },
];

function Row({ keys, label }: { keys: string[]; label: string }) {
  return (
    <li className="flex items-center justify-between gap-4 py-1.5 text-sm">
      <span className="text-foreground">{label}</span>
      <span className="flex shrink-0 gap-1">
        {keys.map((key) => (
          <Kbd key={key}>{key}</Kbd>
        ))}
      </span>
    </li>
  );
}

function Section({ title, rows }: { title: string; rows: { keys: string[]; label: string }[] }) {
  return (
    <section>
      <p className="mb-1 font-mono text-[11px] uppercase tracking-wider text-muted-foreground">{title}</p>
      <ul className="divide-y divide-border">
        {rows.map((row) => (
          <Row key={row.label} {...row} />
        ))}
      </ul>
    </section>
  );
}

export function ShortcutsDialog({ open, onOpenChange, navItems }: ShortcutsDialogProps) {
  const goTo = navItems
    .filter((item) => item.shortcut)
    .map((item) => ({ keys: ["G", item.shortcut!.toUpperCase()], label: `Go to ${item.label}` }));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Keyboard shortcuts</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-5">
          <Section title="General" rows={GENERAL} />
          {goTo.length > 0 ? <Section title="Go to (press G, then the letter)" rows={goTo} /> : null}
          <Section title="Code and review" rows={CODE} />
        </div>
      </DialogContent>
    </Dialog>
  );
}
