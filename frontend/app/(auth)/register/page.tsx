"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import axios from "axios";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";

import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { api } from "@/lib/api";
import { saveSession } from "@/lib/auth";
import type { AuthSession } from "@/types/user";

const registerSchema = z
  .object({
    name: z.string().min(1, "Name is required"),
    email: z.string().email("Enter a valid email address"),
    password: z.string().min(8, "Password must be at least 8 characters"),
    confirmPassword: z.string().min(1, "Confirm your password"),
    role: z.enum(["TUTOR", "STUDENT"], { required_error: "Choose a role" }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ["confirmPassword"],
  });

type RegisterFormValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
  });

  async function onSubmit(values: RegisterFormValues) {
    setServerError(null);
    try {
      const response = await api.post<{ success: boolean; data: AuthSession }>(
        "/api/auth/register",
        {
          name: values.name,
          email: values.email,
          password: values.password,
          role: values.role,
        },
      );
      const session = response.data.data;
      saveSession(session);
      router.push(session.role === "TUTOR" ? "/dashboard" : "/student/dashboard");
    } catch (error) {
      if (axios.isAxiosError<{ message?: string }>(error)) {
        setServerError(error.response?.data?.message ?? "Something went wrong. Try again.");
      } else {
        setServerError("Something went wrong. Try again.");
      }
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">Create your Notare account</CardTitle>
          <CardDescription>Sign up as a teacher or a student to get started.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="name">Name</Label>
              <Input
                id="name"
                autoComplete="name"
                aria-invalid={!!errors.name}
                {...register("name")}
              />
              {errors.name ? <p className="text-xs text-destructive">{errors.name.message}</p> : null}
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                aria-invalid={!!errors.email}
                {...register("email")}
              />
              {errors.email ? (
                <p className="text-xs text-destructive">{errors.email.message}</p>
              ) : null}
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                aria-invalid={!!errors.password}
                {...register("password")}
              />
              {errors.password ? (
                <p className="text-xs text-destructive">{errors.password.message}</p>
              ) : null}
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="confirmPassword">Confirm password</Label>
              <Input
                id="confirmPassword"
                type="password"
                autoComplete="new-password"
                aria-invalid={!!errors.confirmPassword}
                {...register("confirmPassword")}
              />
              {errors.confirmPassword ? (
                <p className="text-xs text-destructive">{errors.confirmPassword.message}</p>
              ) : null}
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="role">I am a</Label>
              <Controller
                control={control}
                name="role"
                render={({ field }) => (
                  <Select value={field.value ?? ""} onValueChange={field.onChange}>
                    <SelectTrigger id="role" className="w-full">
                      <SelectValue placeholder="Choose a role" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="TUTOR">Teacher</SelectItem>
                      <SelectItem value="STUDENT">Student</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.role ? <p className="text-xs text-destructive">{errors.role.message}</p> : null}
            </div>

            {serverError ? (
              <Alert variant="destructive">
                <AlertDescription>{serverError}</AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" disabled={isSubmitting} className="mt-2 w-full">
              {isSubmitting ? "Creating account..." : "Create account"}
            </Button>

            <p className="text-center text-sm text-muted-foreground">
              Already have an account?{" "}
              <Link href="/login" className="text-foreground underline underline-offset-4">
                Sign in
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
