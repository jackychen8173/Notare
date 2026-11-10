"use client";

import { useState } from "react";
import Link from "next/link";
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

const schema = z.object({
  email: z.string().email("Enter a valid email address"),
});

type Values = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  useWakeBackend();
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Values>({ resolver: zodResolver(schema) });

  async function onSubmit(values: Values) {
    setServerError(null);
    try {
      await api.post("/api/auth/password-reset/request", values);
      setSentTo(values.email);
    } catch (error) {
      setServerError(errorMessage(error));
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">Reset your password</CardTitle>
          <CardDescription>
            {sentTo
              ? "Check your inbox."
              : "Enter the email you sign in with, and we'll send you a link to choose a new password."}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {sentTo ? (
            <div className="flex flex-col gap-4">
              <p className="text-sm text-muted-foreground">
                If an account uses <span className="font-medium text-foreground">{sentTo}</span>, a
                reset link is on its way. It works once and expires in 30 minutes. If you don&apos;t
                see it, check your spam folder.
              </p>
              <Link href="/login" className="text-sm text-foreground underline underline-offset-4">
                Back to sign in
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  aria-invalid={!!errors.email}
                  {...register("email")}
                />
                {errors.email ? <p className="text-xs text-destructive">{errors.email.message}</p> : null}
              </div>

              {serverError ? (
                <Alert variant="destructive">
                  <AlertDescription>{serverError}</AlertDescription>
                </Alert>
              ) : null}

              <Button type="submit" disabled={isSubmitting} className="w-full">
                {isSubmitting ? "Sending..." : "Send reset link"}
              </Button>

              <p className="text-center text-sm text-muted-foreground">
                Remembered it?{" "}
                <Link href="/login" className="text-foreground underline underline-offset-4">
                  Sign in
                </Link>
              </p>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
