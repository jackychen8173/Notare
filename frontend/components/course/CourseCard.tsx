import Link from "next/link";
import type { ReactNode } from "react";

import type { Course } from "@/types/course";

interface CourseCardProps {
  course: Course;
  href: string;
  /** Extra line under the subject, e.g. the tutor's name on the student side. */
  meta?: ReactNode;
  footer?: ReactNode;
}

export function CourseCard({ course, href, meta, footer }: CourseCardProps) {
  return (
    <Link
      href={href}
      data-course-color={course.color}
      className="group flex flex-col overflow-hidden rounded-card border border-border bg-card shadow-card transition-all hover:-translate-y-0.5 hover:shadow-elevated"
    >
      <div className="relative h-20 bg-course-solid">
        <div
          aria-hidden
          className="absolute inset-0 bg-[radial-gradient(circle_at_90%_-30%,rgb(255_255_255/0.3),transparent_60%)]"
        />
      </div>
      <div className="flex flex-1 flex-col gap-1 p-4">
        <p className="font-semibold text-foreground group-hover:text-course">{course.name}</p>
        <p className="text-sm text-muted-foreground">{course.subject}</p>
        {meta ? <div className="text-xs text-muted-foreground">{meta}</div> : null}
        {footer ? <div className="mt-3 border-t border-border pt-3 text-xs text-muted-foreground">{footer}</div> : null}
      </div>
    </Link>
  );
}
