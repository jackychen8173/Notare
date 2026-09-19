"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { AdminUser } from "@/types/admin";

export const adminUserKeys = {
  all: ["admin", "users"] as const,
  list: (role?: string, active?: boolean) =>
    ["admin", "users", "list", role ?? null, active ?? null] as const,
  detail: (id: string) => ["admin", "users", id] as const,
};

export interface AdminUserFilters {
  role?: "TUTOR" | "STUDENT";
  active?: boolean;
}

async function fetchAdminUsers(filters: AdminUserFilters): Promise<AdminUser[]> {
  const res = await api.get<ApiEnvelope<AdminUser[]>>("/api/admin/users", { params: filters });
  return res.data.data;
}

async function fetchAdminUser(id: string): Promise<AdminUser> {
  const res = await api.get<ApiEnvelope<AdminUser>>(`/api/admin/users/${id}`);
  return res.data.data;
}

export function useAdminUsers(filters: AdminUserFilters = {}) {
  return useQuery({
    queryKey: adminUserKeys.list(filters.role, filters.active),
    queryFn: () => fetchAdminUsers(filters),
  });
}

export function useAdminUser(id: string) {
  return useQuery({
    queryKey: adminUserKeys.detail(id),
    queryFn: () => fetchAdminUser(id),
    enabled: !!id,
  });
}

export function useDeactivateUser(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.patch<ApiEnvelope<AdminUser>>(`/api/admin/users/${id}/deactivate`);
      return res.data.data;
    },
    onSuccess: (user) => {
      queryClient.setQueryData(adminUserKeys.detail(id), user);
      queryClient.invalidateQueries({ queryKey: adminUserKeys.all });
    },
  });
}

export function useReactivateUser(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.patch<ApiEnvelope<AdminUser>>(`/api/admin/users/${id}/reactivate`);
      return res.data.data;
    },
    onSuccess: (user) => {
      queryClient.setQueryData(adminUserKeys.detail(id), user);
      queryClient.invalidateQueries({ queryKey: adminUserKeys.all });
    },
  });
}
