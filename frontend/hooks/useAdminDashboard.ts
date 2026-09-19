"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { AdminDashboardStats } from "@/types/admin";

export const adminDashboardKeys = {
  stats: ["admin", "dashboard"] as const,
};

async function fetchAdminDashboard(): Promise<AdminDashboardStats> {
  const res = await api.get<ApiEnvelope<AdminDashboardStats>>("/api/admin/dashboard");
  return res.data.data;
}

export function useAdminDashboard() {
  return useQuery({ queryKey: adminDashboardKeys.stats, queryFn: fetchAdminDashboard });
}
