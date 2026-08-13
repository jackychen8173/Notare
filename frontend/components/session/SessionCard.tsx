import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { Session, SessionStatus } from "@/types/session";

const statusVariant: Record<SessionStatus, "default" | "secondary" | "outline"> = {
  SCHEDULED: "secondary",
  COMPLETED: "default",
  CANCELLED: "outline",
};

interface SessionCardProps {
  session: Session;
}

export function SessionCard({ session }: SessionCardProps) {
  return (
    <Link href={`/sessions/${session.id}`}>
      <Card className="transition-colors hover:bg-muted/40">
        <CardContent className="flex items-center justify-between gap-4">
          <div>
            <p className="font-medium text-foreground">{session.subject}</p>
            <p className="text-sm text-muted-foreground">
              {new Date(session.date).toLocaleString()} · {session.duration} min
              {session.courseName ? ` · ${session.courseName}` : ""}
            </p>
          </div>
          <Badge variant={statusVariant[session.status]}>{session.status}</Badge>
        </CardContent>
      </Card>
    </Link>
  );
}
