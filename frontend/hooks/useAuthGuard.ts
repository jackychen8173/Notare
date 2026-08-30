"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { getSession } from "@/lib/auth";
import type { UserRole } from "@/types/user";

export function useAuthGuard(requiredRole: UserRole): boolean {
  const router = useRouter();
  const [authorized, setAuthorized] = useState(false);

  useEffect(() => {
    const session = getSession();
    if (!session) {
      router.replace("/login");
    } else if (session.role !== requiredRole) {
      router.replace(session.role === "TUTOR" ? "/dashboard" : "/student/dashboard");
    } else {
      // getSession() reads localStorage, unavailable during SSR — this can only
      // be determined after mount, so it isn't derivable during render.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setAuthorized(true);
    }
  }, [router, requiredRole]);

  return authorized;
}
