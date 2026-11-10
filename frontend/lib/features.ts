/**
 * True once the backend can actually send email (RESEND_API_KEY and a verified sending domain).
 * Until then, the "Forgot password?" link and the confirm-email notice stay hidden, so nobody is
 * told a link is on its way when none will arrive. The pages themselves still work if visited.
 */
export const emailEnabled = process.env.NEXT_PUBLIC_EMAIL_ENABLED === "true";
