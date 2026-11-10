"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { useProfile, useUpdateProfile } from "@/hooks/useProfile";
import { api, errorMessage } from "@/lib/api";
import { emailEnabled } from "@/lib/features";

const editProfileSchema = z.object({
  name: z.string().min(1, "Name is required"),
});

type EditProfileValues = z.infer<typeof editProfileSchema>;

const roleLabel: Record<"TUTOR" | "STUDENT" | "ADMIN", string> = {
  TUTOR: "Tutor",
  STUDENT: "Student",
  ADMIN: "Admin",
};

/** Shown under the email field until the address is confirmed. */
function ConfirmEmailNotice() {
  const [state, setState] = useState<"idle" | "sending" | "sent">("idle");
  const [error, setError] = useState<string | null>(null);

  async function resend() {
    setState("sending");
    setError(null);
    try {
      await api.post("/api/auth/verify-email/resend");
      setState("sent");
    } catch (e) {
      setError(errorMessage(e));
      setState("idle");
    }
  }

  return (
    <div className="flex flex-col gap-1 rounded-lg bg-warning-surface px-3 py-2 text-xs text-warning">
      <p>
        Not confirmed yet. Confirm it so password-reset emails can reach you.{" "}
        {state === "sent" ? (
          <span>Link sent. Check your inbox.</span>
        ) : (
          <button
            type="button"
            onClick={resend}
            disabled={state === "sending"}
            className="font-medium underline underline-offset-4 disabled:opacity-60"
          >
            {state === "sending" ? "Sending..." : "Send a new link"}
          </button>
        )}
      </p>
      {error ? <p className="text-destructive">{error}</p> : null}
    </div>
  );
}

export function ProfileView() {
  const profile = useProfile();
  const updateProfile = useUpdateProfile();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<EditProfileValues>({
    resolver: zodResolver(editProfileSchema),
    values: profile.data ? { name: profile.data.name } : undefined,
  });

  function onSubmit(values: EditProfileValues) {
    updateProfile.mutate(values, {
      onSuccess: (updated) => reset({ name: updated.name }),
    });
  }

  if (profile.isLoading) {
    return (
      <Card className="max-w-md">
        <CardHeader>
          <Skeleton className="h-6 w-32" />
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Skeleton className="h-9 w-full" />
          <Skeleton className="h-9 w-full" />
        </CardContent>
      </Card>
    );
  }

  if (!profile.data) {
    return <p className="text-sm text-muted-foreground">Couldn&apos;t load your profile.</p>;
  }

  return (
    <Card className="max-w-md">
      <CardHeader>
        <div className="flex items-center gap-2">
          <CardTitle>Profile</CardTitle>
          <Badge variant="outline">{roleLabel[profile.data.role]}</Badge>
        </div>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="name">Name</Label>
            <Input id="name" aria-invalid={!!errors.name} {...register("name")} />
            {errors.name ? <p className="text-xs text-destructive">{errors.name.message}</p> : null}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="email">Email</Label>
            <Input id="email" value={profile.data.email} disabled />
            {emailEnabled && !profile.data.emailVerified && !profile.data.demo ? <ConfirmEmailNotice /> : null}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label>Joined</Label>
            <p className="text-sm text-muted-foreground">
              {new Date(profile.data.createdAt).toLocaleDateString()}
            </p>
          </div>

          <Button type="submit" disabled={!isDirty || updateProfile.isPending} className="mt-2 w-full">
            {updateProfile.isPending ? "Saving..." : "Save changes"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
