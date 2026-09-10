import Link from "next/link";

import { StudentAvatar } from "@/components/student/StudentAvatar";
import { Button } from "@/components/ui/button";
import { TableCell, TableRow } from "@/components/ui/table";
import type { Student } from "@/types/user";

interface StudentRowProps {
  student: Student;
  onRemove?: () => void;
  removePending?: boolean;
}

export function StudentRow({ student, onRemove, removePending }: StudentRowProps) {
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
      {onRemove ? (
        <TableCell>
          <Button variant="ghost" size="sm" disabled={removePending} onClick={onRemove}>
            Remove
          </Button>
        </TableCell>
      ) : null}
    </TableRow>
  );
}
