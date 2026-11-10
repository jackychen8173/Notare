"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useWakeBackend } from "@/hooks/useWakeBackend";
import { api, errorMessage } from "@/lib/api";
import { clearSession } from "@/lib/auth";

const schema = z
  .object({
    newPassword: z.string().min(8, "Use at least 8 characters"),
    confirm: z.string(),
  })
  .refine((v) => v.newPassword === v.confirm, { message: "The passwords don't match", path: ["confirm"] });

type Values = z.infer<typeof schema>;

export default function ResetPasswordPage() {
  useWakeBackend();
  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6">
      <Card className="w-full max-w-sm">
        {/* useSearchParams needs a Suspense boundary so the rest of the page can prerender. */}
        <Suspense fallback={<CardHeader><CardTitle className="text-xl">Choose a new password</CardTitle></CardHeader>}>
          <ResetPasswordForm />
        </Suspense>
      </Card>
    </div>
  );
}

function ResetPasswordForm() {
  const token = useSearchParams().get("token");
  const [done, setDone] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Values>({ resolver: zodResolver(schema) });

  async function onSubmit(values: Values) {
    setServerError(null);
    try {
      await api.post("/api/auth/password-reset/confirm", { token, newPassword: values.newPassword });
      // Every older session was signed out on the server, including any in this browser.
      clearSession();
      setDone(true);
    } catch (error) {
      setServerError(errorMessage(error));
    }
  }

  if (!token) {
    return (
      <>
        <CardHeader>
          <CardTitle className="text-xl">This link is incomplete</CardTitle>
          <CardDescription>Open the link from the email again, or request a new one.</CardDescription>
        </CardHeader>
        <CardContent>
          <Link href="/forgot-password" className="text-sm text-foreground underline underline-offset-4">
            Request a new link
          </Link>
        </CardContent>
      </>
    );
  }

  if (done) {
    return (
      <>
        <CardHeader>
          <CardTitle className="text-xl">Password updated</CardTitle>
          <CardDescription>
            You&apos;ve been signed out everywhere. Sign in with your new password.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button className="w-full" nativeButton={false} render={<Link href="/login" />}>
            Sign in
          </Button>
        </CardContent>
      </>
    );
  }

  return (
    <>
      <CardHeader>
        <CardTitle className="text-xl">Choose a new password</CardTitle>
        <CardDescription>This also signs you out on every other device.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="newPassword">New password</Label>
            <Input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors.newPassword}
              {...register("newPassword")}
            />
            {errors.newPassword ? <p className="text-xs text-destructive">{errors.newPassword.message}</p> : null}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="confirm">Confirm new password</Label>
            <Input
              id="confirm"
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors.confirm}
              {...register("confirm")}
            />
            {errors.confirm ? <p className="text-xs text-destructive">{errors.confirm.message}</p> : null}
          </div>

          {serverError ? (
            <Alert variant="destructive">
              <AlertDescription>
                {serverError}{" "}
                <Link href="/forgot-password" className="underline underline-offset-4">
                  Request a new link
                </Link>
              </AlertDescription>
            </Alert>
          ) : null}

          <Button type="submit" disabled={isSubmitting} className="w-full">
            {isSubmitting ? "Saving..." : "Save new password"}
          </Button>
        </form>
      </CardContent>
    </>
  );
}
