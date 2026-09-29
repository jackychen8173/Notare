"use client";

import { Suspense, use } from "react";

import { StudentAnnouncementsSection } from "@/components/course/AnnouncementsSection";
import { ClassworkByTopic } from "@/components/course/ClassworkByTopic";
import { CourseBanner } from "@/components/course/CourseBanner";
import { CourseTabs, useCourseTab, type CourseTab } from "@/components/course/CourseTabs";
import { StudentMaterialsSection } from "@/components/course/MaterialsSection";
import { UpcomingCard } from "@/components/course/UpcomingCard";
import { MyUnitProgress } from "@/components/progress/UnitProgress";
import { DiscussionsSection } from "@/components/discussion/DiscussionsSection";
import { Breadcrumbs } from "@/components/layout/Breadcrumbs";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyCourseAssignments } from "@/hooks/useAssignments";
import { useMyCourse } from "@/hooks/useCourses";
import { useDiscussionThreads } from "@/hooks/useDiscussions";
import { useMyCourseQuizzes } from "@/hooks/useQuizzes";

const TABS: CourseTab[] = [
  { id: "stream", label: "Stream" },
  { id: "classwork", label: "Classwork" },
  { id: "progress", label: "Progress" },
  { id: "discussions", label: "Discussions" },
];

function StudentCourseDetail({ id }: { id: string }) {
  const course = useMyCourse(id);
  const assignments = useMyCourseAssignments(id);
  const quizzes = useMyCourseQuizzes(id);
  const threads = useDiscussionThreads("student", id);
  const unread = threads.data?.filter((thread) => thread.unread).length ?? 0;
  const tabs = TABS.map((tab) => (tab.id === "discussions" ? { ...tab, badge: unread } : tab));
  const tab = useCourseTab(tabs);

  if (course.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-4 w-40" />
        <Skeleton className="h-36 w-full rounded-card" />
        <Skeleton className="h-10 w-full" />
      </div>
    );
  }

  if (!course.data) {
    return <p className="text-sm text-muted-foreground">Course not found.</p>;
  }

  const assignmentList = assignments.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Breadcrumbs items={[{ label: "Courses", href: "/student/courses" }, { label: course.data.name }]} />
        <CourseBanner course={course.data} />
      </div>

      <CourseTabs tabs={tabs} active={tab} />

      {tab === "stream" ? (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
          <StudentAnnouncementsSection courseId={id} />
          <UpcomingCard
            assignments={assignmentList}
            href={(assignment) => `/student/assignments/${assignment.id}`}
          />
        </div>
      ) : null}

      {tab === "classwork" ? (
        <div className="flex flex-col gap-10">
          {assignments.isLoading || quizzes.isLoading ? (
            <Skeleton className="h-24 w-full" />
          ) : (
            <ClassworkByTopic
              assignments={assignmentList}
              quizzes={quizzes.data ?? []}
              assignmentHref={(assignment) => `/student/assignments/${assignment.id}`}
              quizHref={(quiz) => `/student/quizzes/${quiz.id}`}
              showQuizStatus={false}
              emptyDescription="Your tutor hasn't posted any assignments or quizzes yet."
            />
          )}
          <StudentMaterialsSection courseId={id} />
        </div>
      ) : null}

      {tab === "progress" ? <MyUnitProgress courseId={id} /> : null}

      {tab === "discussions" ? (
        <DiscussionsSection scope="student" courseId={id} archived={course.data.archivedAt !== null} />
      ) : null}
    </div>
  );
}

export default function StudentCourseDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  // useSearchParams (the active tab) needs a Suspense boundary.
  return (
    <Suspense fallback={<Skeleton className="h-36 w-full rounded-card" />}>
      <StudentCourseDetail id={id} />
    </Suspense>
  );
}
