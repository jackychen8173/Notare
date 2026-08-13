export default function Home() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-6 bg-background p-8 text-center">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-medium text-foreground">Notare</h1>
        <p className="max-w-sm text-sm text-muted-foreground">
          Tutor and student screens haven&apos;t been built yet — this page just
          confirms the design tokens (fonts, colors, radii) are wired up.
        </p>
      </div>

      <div className="flex items-center gap-3 rounded-card border-hairline border-border bg-card px-6 py-4">
        <span className="rounded-full bg-primary px-3 py-1 text-xs font-medium text-primary-foreground">
          Primary
        </span>
        <span className="rounded-full border-hairline border-sage-border bg-sage-surface px-3 py-1 text-xs font-medium text-sage-text">
          Sage (AI only)
        </span>
        <span className="font-mono text-xs text-muted-foreground">JetBrains Mono</span>
      </div>
    </div>
  );
}
