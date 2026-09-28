/** Inline explanation next to a Sage or Run control that's disabled for demo accounts. */
export function DemoDisabledNote({ feature = "Sage" }: { feature?: string }) {
  return <p className="text-xs text-muted-foreground">{feature} is turned off in the demo.</p>;
}
