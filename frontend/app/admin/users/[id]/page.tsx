"use client";

import { use } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminUser, useDeactivateUser, useReactivateUser } from "@/hooks/useAdminUsers";

export default function AdminUserDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const user = useAdminUser(id);
  const deactivate = useDeactivateUser(id);
  const reactivate = useReactivateUser(id);

  if (user.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (!user.data) {
    return <p className="text-sm text-muted-foreground">User not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={user.data.name}
        description={user.data.email}
        actions={
          user.data.active ? (
            <Button
              variant="outline"
              disabled={deactivate.isPending}
              onClick={() => deactivate.mutate()}
            >
              {deactivate.isPending ? "Deactivating..." : "Deactivate"}
            </Button>
          ) : (
            <Button
              variant="outline"
              disabled={reactivate.isPending}
              onClick={() => reactivate.mutate()}
            >
              {reactivate.isPending ? "Reactivating..." : "Reactivate"}
            </Button>
          )
        }
      />

      <Card>
        <CardContent className="flex flex-col gap-3">
          <div className="flex items-center gap-2">
            <Badge variant="outline">{user.data.role === "TUTOR" ? "Tutor" : "Student"}</Badge>
            <Badge variant={user.data.active ? "secondary" : "destructive"}>
              {user.data.active ? "Active" : "Deactivated"}
            </Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            Joined {new Date(user.data.createdAt).toLocaleDateString()}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
