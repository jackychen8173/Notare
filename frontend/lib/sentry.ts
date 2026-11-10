// Shared Sentry settings for the browser, Node and edge runtimes. The SDK does nothing until
// NEXT_PUBLIC_SENTRY_DSN is set. Errors only: no tracing or session replay (they would spend the
// free quota, and students are often minors), and no personal data (sendDefaultPii stays off).
export const sentryOptions = {
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
  enabled: Boolean(process.env.NEXT_PUBLIC_SENTRY_DSN),
  environment: process.env.NEXT_PUBLIC_VERCEL_ENV ?? "local",
  sendDefaultPii: false,
};
