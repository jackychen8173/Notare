import { getSession } from "@/lib/auth";

/** "Good morning, Jamie" style heading for dashboards. Client-only (reads the stored session). */
export function greeting(): string {
  const hour = new Date().getHours();
  const part = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
  const firstName = getSession()?.name?.split(" ")[0];
  return firstName ? `${part}, ${firstName}` : part;
}
