// Calendar subscribe feeds (/api/calendar/<token>.ics). A thin proxy to the backend's public
// /api/calendar-feeds/<token>, which owns everything about the feed; this route only adds a CDN
// cache. Calendar apps poll their feeds on a timer, and every request that reached the backend
// would wake it from its idle sleep (see CLAUDE.md), so Vercel's CDN answers repeat polls: the
// backend sees at most one request per feed every few hours.
export const runtime = "nodejs";

const CACHE_CONTROL = "public, max-age=3600, s-maxage=21600, stale-while-revalidate=43200";
// The token is 43 base64url characters; anything else can't be a real feed.
const TOKEN = /^[A-Za-z0-9_-]{20,128}$/;

export async function GET(_request: Request, { params }: { params: Promise<{ token: string }> }) {
  const { token: raw } = await params;
  const token = raw.endsWith(".ics") ? raw.slice(0, -4) : raw;
  const apiUrl = process.env.NEXT_PUBLIC_API_URL;
  if (!TOKEN.test(token) || !apiUrl) {
    return new Response("Calendar not found", { status: 404 });
  }

  const upstream = await fetch(`${apiUrl}/api/calendar-feeds/${encodeURIComponent(token)}`, { cache: "no-store" });
  if (upstream.status === 404) {
    // Cached for a day too, so an old subscription still polling a reset link doesn't wake the backend each time.
    return new Response("Calendar not found", {
      status: 404,
      headers: { "Cache-Control": "public, max-age=3600, s-maxage=86400" },
    });
  }
  if (!upstream.ok) {
    return new Response("Calendar unavailable, try again later", { status: 502, headers: { "Cache-Control": "no-store" } });
  }

  return new Response(await upstream.text(), {
    status: 200,
    headers: {
      "Content-Type": "text/calendar; charset=utf-8",
      "Content-Disposition": 'inline; filename="notare.ics"',
      "Cache-Control": CACHE_CONTROL,
      "X-Robots-Tag": "noindex",
    },
  });
}
