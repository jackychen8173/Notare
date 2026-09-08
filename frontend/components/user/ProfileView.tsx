"use client";

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

const editProfileSchema = z.object({
  name: z.string().min(1, "Name is required"),
});

type EditProfileValues = z.infer<typeof editProfileSchema>;

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
          <Badge variant="outline">{profile.data.role === "TUTOR" ? "Tutor" : "Student"}</Badge>
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
