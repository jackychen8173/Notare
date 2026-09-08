"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { IconBackpack, IconChalkboard, IconCheck } from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getSession } from "@/lib/auth";

const teacherFeatures = [
  "Manage students and courses",
  "Schedule sessions and keep notes",
  "Review assignments and submissions",
];

const studentFeatures = [
  "View your courses and assignments",
  "Submit your work",
  "Track your session history",
  "See feedback and grades once your tutor releases them",
];

export default function Home() {
  const router = useRouter();
  const [checkingSession, setCheckingSession] = useState(true);

  useEffect(() => {
    const session = getSession();
    if (session) {
      router.replace(session.role === "TUTOR" ? "/dashboard" : "/student/dashboard");
    } else {
      // getSession() reads localStorage, unavailable during SSR — this can only
      // be determined after mount, so it isn't derivable during render.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setCheckingSession(false);
    }
  }, [router]);

  if (checkingSession) return null;

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col items-center gap-12 px-6 py-16">
      <div className="flex flex-col items-center gap-3 text-center">
        <h1 className="text-3xl font-medium text-foreground">Notare</h1>
        <p className="max-w-md text-muted-foreground">
          Tutoring management with Sage, an AI assistant that drafts session notes and
          assignment feedback for tutors to review before students ever see them.
        </p>
      </div>

      <div className="grid w-full gap-6 sm:grid-cols-2">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <IconChalkboard className="size-5 text-muted-foreground" stroke={1.75} />
              <CardTitle>For Teachers</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="flex h-full flex-col gap-4">
            <ul className="flex flex-col gap-2 text-sm">
              {teacherFeatures.map((feature) => (
                <li key={feature} className="flex items-start gap-2">
                  <IconCheck className="mt-0.5 size-4 shrink-0 text-muted-foreground" stroke={1.75} />
                  <span>{feature}</span>
                </li>
              ))}
              <li className="flex items-start gap-2 rounded-card border-hairline border-sage-border bg-sage-surface p-2">
                <IconCheck className="mt-0.5 size-4 shrink-0 text-sage-text" stroke={1.75} />
                <span className="text-sage-text">
                  Get Sage AI-drafted feedback you approve before students see it
                </span>
              </li>
            </ul>
            <div className="mt-auto flex gap-2">
              <Button nativeButton={false} render={<Link href="/register" />} className="flex-1">
                Sign up
              </Button>
              <Button
                nativeButton={false}
                render={<Link href="/login" />}
                variant="outline"
                className="flex-1"
              >
                Sign in
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <IconBackpack className="size-5 text-muted-foreground" stroke={1.75} />
              <CardTitle>For Students</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="flex h-full flex-col gap-4">
            <ul className="flex flex-col gap-2 text-sm">
              {studentFeatures.map((feature) => (
                <li key={feature} className="flex items-start gap-2">
                  <IconCheck className="mt-0.5 size-4 shrink-0 text-muted-foreground" stroke={1.75} />
                  <span>{feature}</span>
                </li>
              ))}
            </ul>
            <div className="mt-auto flex gap-2">
              <Button nativeButton={false} render={<Link href="/register" />} className="flex-1">
                Sign up
              </Button>
              <Button
                nativeButton={false}
                render={<Link href="/login" />}
                variant="outline"
                className="flex-1"
              >
                Sign in
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
