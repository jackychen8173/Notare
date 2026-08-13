import Link from "next/link";

import { StudentAvatar } from "@/components/student/StudentAvatar";
import { TableCell, TableRow } from "@/components/ui/table";
import type { Student } from "@/types/user";

interface StudentRowProps {
  student: Student;
}

export function StudentRow({ student }: StudentRowProps) {
  return (
    <TableRow>
      <TableCell>
        <Link href={`/students/${student.id}`} className="flex items-center gap-3">
          <StudentAvatar name={student.name} size="sm" />
          <span className="font-medium text-foreground">{student.name}</span>
        </Link>
      </TableCell>
      <TableCell className="text-muted-foreground">{student.email}</TableCell>
      <TableCell className="text-muted-foreground">
        {new Date(student.createdAt).toLocaleDateString()}
      </TableCell>
    </TableRow>
  );
}
