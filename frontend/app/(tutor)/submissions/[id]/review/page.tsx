"use client";

import { use, useEffect } from "react";
import { useRouter } from "next/navigation";
import { IconChevronLeft, IconChevronRight } from "@tabler/icons-react";

import { Breadcrumbs } from "@/components/layout/Breadcrumbs";
import { useAssignment } from "@/hooks/useAssignments";
import { SubmissionReview } from "@/components/assignment/SubmissionReview";
import { Button } from "@/components/ui/button";
import { Kbd } from "@/components/ui/kbd";
import { Skeleton } from "@/components/ui/skeleton";
import { useTutorHome } from "@/hooks/useHome";
import { useAssignmentSubmissions, useSubmission } from "@/hooks/useSubmissions";

/** True when a keypress should go to a text field rather than a page shortcut. */
function isTyping(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) return false;
  return target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName);
}

export default function SubmissionReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const submission = useSubmission(id);
  const assignment = useAssignment(submission.data?.assignmentId ?? "");
  const allSubmissions = useAssignmentSubmissions(submission.data?.assignmentId ?? "");
  const home = useTutorHome();

  // The review queue (unreleased work, oldest first) drives previous/next. A submission that's
  // already released isn't in it, so "next" from there is simply the first thing still waiting.
  const queue = home.data?.submissions ?? [];
  const position = queue.findIndex((item) => item.submissionId === id);
  const prev = position > 0 ? queue[position - 1] : undefined;
  const next = position >= 0 ? queue[position + 1] : queue.find((item) => item.submissionId !== id);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.metaKey || event.ctrlKey || event.altKey || isTyping(event.target)) return;
      if (event.key === "j" && next) {
        router.push(`/submissions/${next.submissionId}/review`);
      } else if (event.key === "k" && prev) {
        router.push(`/submissions/${prev.submissionId}/review`);
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [router, next, prev]);

  if (submission.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!submission.data) {
    return <p className="text-sm text-muted-foreground">Submission not found.</p>;
  }

  // The list may be cached from before this version existed, so always include the one on screen.
  const versions = [
    submission.data,
    ...(allSubmissions.data ?? []).filter(
      (s) => s.studentId === submission.data.studentId && s.id !== submission.data.id,
    ),
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <Breadcrumbs
            items={[
              { label: "Courses", href: "/courses" },
              ...(assignment.data
                ? [
                    { label: assignment.data.courseName, href: `/courses/${assignment.data.courseId}?tab=classwork` },
                    { label: assignment.data.title, href: `/assignments/${assignment.data.id}` },
                  ]
                : []),
              { label: submission.data.studentName },
            ]}
          />
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">{submission.data.studentName}</h1>
          <p className="text-sm text-muted-foreground">
            {assignment.data?.title ?? "Submission"} · submitted {new Date(submission.data.submittedAt).toLocaleString()}
          </p>
        </div>

        {queue.length > 0 ? (
          <div className="flex items-center gap-2">
            <span className="hidden items-center gap-1 text-xs text-muted-foreground lg:flex">
              <Kbd>K</Kbd>
              <Kbd>J</Kbd>
            </span>
            <span className="font-mono text-xs text-muted-foreground">
              {position >= 0 ? `${position + 1}/${queue.length} in queue` : `${queue.length} waiting`}
            </span>
            <Button
              size="icon-sm"
              variant="outline"
              aria-label="Previous submission (K)"
              disabled={!prev}
              onClick={() => prev && router.push(`/submissions/${prev.submissionId}/review`)}
            >
              <IconChevronLeft />
            </Button>
            <Button
              size="sm"
              variant="outline"
              aria-label="Next submission (J)"
              disabled={!next}
              onClick={() => next && router.push(`/submissions/${next.submissionId}/review`)}
            >
              Next <Kbd>J</Kbd>
              <IconChevronRight />
            </Button>
          </div>
        ) : null}
      </div>
      <SubmissionReview key={submission.data.id} submission={submission.data} versions={versions} />
    </div>
  );
}
