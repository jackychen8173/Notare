"use client";

import { PageHeader } from "@/components/layout/PageHeader";
import { StudentRow } from "@/components/student/StudentRow";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useStudents } from "@/hooks/useStudents";

export default function StudentsPage() {
  const students = useStudents();

  return (
    <>
      <PageHeader
        title="Students"
        description="Students enrolled in one of your courses."
      />

      {students.isLoading ? (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
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
              <StudentRow key={student.id} student={student} />
            ))}
          </TableBody>
        </Table>
      ) : (
        <p className="text-sm text-muted-foreground">No students yet.</p>
      )}
    </>
  );
}
