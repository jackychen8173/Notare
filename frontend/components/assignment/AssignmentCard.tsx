import Link from "next/link";

import { Card, CardContent } from "@/components/ui/card";
import type { Assignment } from "@/types/assignment";

interface AssignmentCardProps {
  assignment: Assignment;
  /** Defaults to the tutor detail route; pass the student route explicitly there. */
  href?: string;
}

export function AssignmentCard({ assignment, href }: AssignmentCardProps) {
  return (
    <Link href={href ?? `/assignments/${assignment.id}`}>
      <Card className="transition-colors hover:bg-muted/40">
        <CardContent>
          <p className="font-medium text-foreground">{assignment.title}</p>
          <p className="text-sm text-muted-foreground">
            Due {new Date(assignment.dueDate).toLocaleDateString()}
          </p>
        </CardContent>
      </Card>
    </Link>
  );
}
