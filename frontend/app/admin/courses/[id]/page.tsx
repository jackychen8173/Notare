"use client";

import { use } from "react";
import Link from "next/link";

import { IconFileText, IconUsers } from "@tabler/icons-react";

import { EmptyState } from "@/components/layout/EmptyState";
import { StudentRow } from "@/components/student/StudentRow";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import {
  useAdminCourse,
  useAdminCourseAssignments,
  useAdminCourseStudents,
} from "@/hooks/useAdminCourses";
import { formatDueDate } from "@/lib/dates";

export default function AdminCourseDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const course = useAdminCourse(id);
  const students = useAdminCourseStudents(id);
  const assignments = useAdminCourseAssignments(id);

  if (course.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (!course.data) {
    return <p className="text-sm text-muted-foreground">Course not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">{course.data.name}</h1>
        <p className="text-sm text-muted-foreground">
          {course.data.subject} · Taught by {course.data.tutorName}
        </p>
        {course.data.description ? (
          <p className="mt-2 text-sm text-muted-foreground">{course.data.description}</p>
        ) : null}
      </div>

      <div>
        <h2 className="mb-3 text-lg font-semibold text-foreground">Enrolled students</h2>
        {students.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : students.data && students.data.length > 0 ? (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Added</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {students.data.map((student) => (
                <StudentRow key={student.id} student={student} href={`/admin/users/${student.id}`} />
              ))}
            </TableBody>
          </Table>
        ) : (
          <EmptyState icon={IconUsers} title="No students enrolled yet" />
        )}
      </div>

      <div>
        <h2 className="mb-3 text-lg font-semibold text-foreground">Assignments</h2>
        {assignments.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : assignments.data && assignments.data.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2">
            {assignments.data.map((assignment) => (
              <Link key={assignment.id} href={`/admin/assignments/${assignment.id}`}>
                <Card className="transition-all hover:-translate-y-px hover:shadow-elevated">
                  <CardContent>
                    <p className="font-medium text-foreground">{assignment.title}</p>
                    <p className="text-sm text-muted-foreground">
                      Due {formatDueDate(assignment.dueDate)}
                    </p>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        ) : (
          <EmptyState icon={IconFileText} title="No assignments yet" />
        )}
      </div>
    </div>
  );
}
