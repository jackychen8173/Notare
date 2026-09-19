"use client";

import { use } from "react";
import Link from "next/link";

import { StudentRow } from "@/components/student/StudentRow";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import {
  useAdminCourse,
  useAdminCourseAssignments,
  useAdminCourseStudents,
} from "@/hooks/useAdminCourses";

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
        <h1 className="text-2xl font-medium text-foreground">{course.data.name}</h1>
        <p className="text-sm text-muted-foreground">
          {course.data.subject} · Taught by {course.data.tutorName}
        </p>
        {course.data.description ? (
          <p className="mt-2 text-sm text-muted-foreground">{course.data.description}</p>
        ) : null}
      </div>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Enrolled students</h2>
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
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No students enrolled yet.</p>
            </CardContent>
          </Card>
        )}
      </div>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Assignments</h2>
        {assignments.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : assignments.data && assignments.data.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2">
            {assignments.data.map((assignment) => (
              <Link key={assignment.id} href={`/admin/assignments/${assignment.id}`}>
                <Card className="transition-colors hover:bg-muted/40">
                  <CardContent>
                    <p className="font-medium text-foreground">{assignment.title}</p>
                    <p className="text-sm text-muted-foreground">
                      Due {new Date(assignment.dueDate).toLocaleDateString()}
                    </p>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No assignments yet.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
