"use client";

import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyRubric } from "@/hooks/useRubrics";

export function RubricView({ assignmentId }: { assignmentId: string }) {
  const rubric = useMyRubric(assignmentId);

  if (rubric.isLoading) {
    return <Skeleton className="h-16 w-full" />;
  }

  if (!rubric.data) {
    return null;
  }

  return (
    <div>
      <h2 className="mb-3 text-lg font-medium text-foreground">Grading rubric</h2>
      <Card>
        <CardContent className="flex flex-col gap-3">
          <p className="font-medium text-foreground">{rubric.data.title}</p>
          <div className="flex flex-col gap-2">
            {rubric.data.criteria.map((criterion) => (
              <div
                key={criterion.id}
                className="flex items-start justify-between gap-4 border-t-hairline border-border pt-2 first:border-t-0 first:pt-0"
              >
                <div>
                  <p className="text-sm font-medium text-foreground">{criterion.name}</p>
                  {criterion.description ? (
                    <p className="text-xs text-muted-foreground">{criterion.description}</p>
                  ) : null}
                </div>
                <p className="whitespace-nowrap text-sm text-muted-foreground">{criterion.pointsPossible} pts</p>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
