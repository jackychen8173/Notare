"use client";

import { useEffect, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { ApprovalPreview } from "@/components/landing/ApprovalPreview";
import { DemoButtons } from "@/components/landing/DemoButtons";
import {
  CalendarFragment,
  DiscussionFragment,
  QuizFragment,
  RunFragment,
} from "@/components/landing/FeatureFragments";
import { BrandMark } from "@/components/layout/Sidebar";
import { ThemeToggle } from "@/components/layout/ThemeToggle";
import { Button } from "@/components/ui/button";
import { getSession } from "@/lib/auth";
import type { UserRole } from "@/types/user";

const ROLE_HOME: Record<UserRole, string> = {
  TUTOR: "/dashboard",
  STUDENT: "/student/dashboard",
  ADMIN: "/admin/dashboard",
};

const FEATURES: { title: string; body: string; fragment: ReactNode }[] = [
  {
    title: "Java that runs in the browser",
    body: "Students write in a real code editor and press Run. Their code compiles and runs in an isolated sandbox, so they see output and errors before they submit.",
    fragment: <RunFragment />,
  },
  {
    title: "Every section on one calendar",
    body: "Give each class its meeting times and a color. Class meetings, due dates and one-on-one sessions show up on a shared calendar for you and your students.",
    fragment: <CalendarFragment />,
  },
  {
    title: "Quizzes that grade themselves",
    body: "Multiple choice and true/false are scored the moment a student submits. Short answers come to you, with a suggested score from Sage when you want one.",
    fragment: <QuizFragment />,
  },
  {
    title: "A place for the questions students hesitate to ask",
    body: "Each course has a forum where students can post anonymously to classmates, plus private threads that go straight to you.",
    fragment: <DiscussionFragment />,
  },
];

export default function Home() {
  const router = useRouter();

  // Signed-in visitors go straight to their app. The landing page renders immediately for everyone
  // else rather than waiting on this check, since most visitors here aren't signed in.
  useEffect(() => {
    const session = getSession();
    if (session) router.replace(ROLE_HOME[session.role]);
  }, [router]);

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="mx-auto flex w-full max-w-6xl items-center justify-between px-4 py-4 sm:px-6">
        <BrandMark />
        <nav className="flex items-center gap-1 sm:gap-2">
          <ThemeToggle />
          <Button nativeButton={false} variant="ghost" render={<Link href="/login" />}>
            Sign in
          </Button>
          <Button nativeButton={false} variant="outline" render={<Link href="/register" />} className="hidden sm:inline-flex">
            Create account
          </Button>
        </nav>
      </header>

      <main className="flex-1">
        <section className="mx-auto grid w-full max-w-6xl grid-cols-1 items-center gap-12 px-4 pt-10 pb-20 sm:px-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)] lg:gap-16 lg:pt-16 lg:pb-28">
          <div className="flex min-w-0 flex-col gap-6">
            <h1 className="max-w-xl text-4xl leading-[1.08] font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-5xl lg:text-[3.4rem]">
              Feedback on student code, drafted by AI and approved by you.
            </h1>
            <p className="max-w-lg text-lg leading-relaxed text-muted-foreground">
              Notare is a classroom for AP Computer Science A. Assign Java, give quizzes, keep a class calendar,
              and let Sage draft feedback that students only see after you&apos;ve checked it.
            </p>
            <DemoButtons className="mt-2" />
          </div>
          <ApprovalPreview />
        </section>

        <section className="border-t border-border bg-card/60">
          <div className="mx-auto flex w-full max-w-6xl flex-col gap-16 px-4 py-20 sm:px-6 lg:gap-24 lg:py-28">
            <h2 className="max-w-2xl text-3xl font-semibold tracking-tight text-balance text-foreground sm:text-4xl">
              Everything else a CS class runs on, in the same place.
            </h2>
            {FEATURES.map((feature, index) => (
              <div
                key={feature.title}
                className="grid grid-cols-1 items-center gap-8 md:grid-cols-2 md:gap-14"
              >
                <div className={index % 2 === 1 ? "md:order-2" : undefined}>
                  <h3 className="text-xl font-semibold tracking-tight text-foreground">{feature.title}</h3>
                  <p className="mt-3 max-w-md leading-relaxed text-muted-foreground">{feature.body}</p>
                </div>
                <div className="w-full max-w-md md:justify-self-center">{feature.fragment}</div>
              </div>
            ))}
          </div>
        </section>

        <section className="mx-auto flex w-full max-w-6xl flex-col items-start gap-6 px-4 py-20 sm:px-6 lg:py-24">
          <h2 className="max-w-2xl text-3xl font-semibold tracking-tight text-balance text-foreground sm:text-4xl">
            See it with a class already in progress.
          </h2>
          <p className="max-w-xl leading-relaxed text-muted-foreground">
            Each demo is your own private copy: a teacher, four students, two sections, and a few weeks of
            assignments, quizzes and discussions. Nothing you change there affects anyone else.
          </p>
          <DemoButtons />
        </section>
      </main>

      <footer className="border-t border-border">
        <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-6 text-sm text-muted-foreground sm:px-6">
          <BrandMark />
          <div className="flex gap-5">
            <Link href="/login" className="hover:text-foreground">
              Sign in
            </Link>
            <Link href="/register" className="hover:text-foreground">
              Create account
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
