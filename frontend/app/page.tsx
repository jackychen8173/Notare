"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { getSession } from "@/lib/auth";

export default function Home() {
  const router = useRouter();

  useEffect(() => {
    const session = getSession();
    if (!session) {
      router.replace("/login");
    } else {
      router.replace(session.role === "TUTOR" ? "/dashboard" : "/student/dashboard");
    }
  }, [router]);

  return null;
}
