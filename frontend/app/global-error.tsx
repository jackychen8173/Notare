"use client";

import * as Sentry from "@sentry/nextjs";
import { useEffect } from "react";

import "./globals.css";

// Shown when rendering fails outside any nested error boundary. It replaces the root layout, so
// it brings its own <html> and stylesheet, and reports the error to Sentry (a no-op without a DSN).
export default function GlobalError({
  error,
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  useEffect(() => {
    Sentry.captureException(error);
  }, [error]);

  return (
    <html lang="en">
      <body className="min-h-screen bg-background text-foreground flex items-center justify-center p-6 font-sans antialiased">
        <title>Something went wrong · Notare</title>
        <div className="max-w-sm text-center space-y-3">
          <h1 className="text-lg font-medium">Something went wrong</h1>
          <p className="text-sm text-muted-foreground">
            The page hit an error. Try again, and if it keeps happening, reload the page.
          </p>
          <button
            type="button"
            onClick={() => retry()}
            className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground"
          >
            Try again
          </button>
        </div>
      </body>
    </html>
  );
}
