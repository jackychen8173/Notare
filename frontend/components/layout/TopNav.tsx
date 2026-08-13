import { IconUserCircle } from "@tabler/icons-react";

export function TopNav() {
  return (
    <header className="flex h-14 shrink-0 items-center justify-end border-b-hairline border-border bg-background px-6">
      <button
        type="button"
        className="flex size-8 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        aria-label="Account"
      >
        <IconUserCircle className="size-5" stroke={1.75} />
      </button>
    </header>
  );
}
