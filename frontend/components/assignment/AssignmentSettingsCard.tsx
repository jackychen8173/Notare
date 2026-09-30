"use client";

import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useUpdateAssignmentSettings } from "@/hooks/useAssignments";
import { useTopics } from "@/hooks/useTopics";
import { errorMessage } from "@/lib/api";
import type { Assignment } from "@/types/assignment";

const NO_UNIT = "__none__";

/** Resubmission and unit for an existing assignment. Each change saves immediately. */
export function AssignmentSettingsCard({ assignment }: { assignment: Assignment }) {
  const topics = useTopics(assignment.courseId);
  const update = useUpdateAssignmentSettings(assignment.id);

  function save(changes: Partial<{ allowResubmission: boolean; topicId: string | null }>) {
    update.mutate({
      allowResubmission: assignment.allowResubmission,
      topicId: assignment.topicId,
      ...changes,
    });
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <label className="flex items-start gap-2 text-sm text-foreground">
          <input
            type="checkbox"
            className="mt-1 accent-primary"
            checked={assignment.allowResubmission}
            disabled={update.isPending}
            onChange={(event) => save({ allowResubmission: event.target.checked })}
          />
          <span>
            Allow resubmission
            <span className="block text-xs text-muted-foreground">
              Students can submit a new version once you release feedback.
            </span>
          </span>
        </label>
        <div className="flex items-center gap-2">
          <Label htmlFor="assignment-unit" className="text-sm text-muted-foreground">
            Unit
          </Label>
          <Select
            value={assignment.topicId ?? NO_UNIT}
            onValueChange={(value) => save({ topicId: value === NO_UNIT ? null : (value as string) })}
          >
            <SelectTrigger id="assignment-unit" className="w-48">
              <SelectValue placeholder="No unit">
                {(value: string) =>
                  value === NO_UNIT ? "No unit" : (topics.data?.find((t) => t.id === value)?.name ?? assignment.topicName)
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NO_UNIT}>No unit</SelectItem>
              {topics.data?.map((topic) => (
                <SelectItem key={topic.id} value={topic.id}>
                  {topic.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </CardContent>
      {update.isError ? <p className="px-6 pb-4 text-xs text-destructive">{errorMessage(update.error)}</p> : null}
    </Card>
  );
}
