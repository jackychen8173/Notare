"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import {
  useAnnouncements,
  useCreateAnnouncement,
  useDeleteAnnouncement,
  useMyCourseAnnouncements,
} from "@/hooks/useAnnouncements";
import type { Announcement } from "@/types/announcement";

function NewAnnouncementForm({ courseId }: { courseId: string }) {
  const [content, setContent] = useState("");
  const createAnnouncement = useCreateAnnouncement(courseId);

  function onSubmit() {
    const trimmed = content.trim();
    if (!trimmed) return;
    createAnnouncement.mutate(trimmed, { onSuccess: () => setContent("") });
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <Textarea
          rows={3}
          placeholder="Post an announcement to the class..."
          value={content}
          onChange={(event) => setContent(event.target.value)}
        />
        <Button
          type="button"
          size="sm"
          className="self-end"
          disabled={createAnnouncement.isPending || !content.trim()}
          onClick={onSubmit}
        >
          {createAnnouncement.isPending ? "Posting..." : "Post"}
        </Button>
      </CardContent>
    </Card>
  );
}

function AnnouncementCard({ announcement, editable, onDelete, deletePending }: {
  announcement: Announcement;
  editable: boolean;
  onDelete?: () => void;
  deletePending?: boolean;
}) {
  return (
    <Card>
      <CardContent>
        <div className="mb-1 flex items-center justify-between">
          <p className="text-xs text-muted-foreground">
            {announcement.tutorName} · {new Date(announcement.createdAt).toLocaleString()}
          </p>
          {editable ? (
            <Button variant="outline" size="sm" disabled={deletePending} onClick={onDelete}>
              Delete
            </Button>
          ) : null}
        </div>
        <p className="whitespace-pre-wrap text-sm text-foreground">{announcement.content}</p>
      </CardContent>
    </Card>
  );
}

export function AnnouncementsSection({ courseId, editable }: { courseId: string; editable: boolean }) {
  const announcements = useAnnouncements(courseId);
  const deleteAnnouncement = useDeleteAnnouncement(courseId);

  return (
    <div>
      <h2 className="mb-3 text-lg font-medium text-foreground">Announcements</h2>
      <div className="flex flex-col gap-3">
        {editable ? <NewAnnouncementForm courseId={courseId} /> : null}
        {announcements.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : announcements.data && announcements.data.length > 0 ? (
          announcements.data.map((announcement) => (
            <AnnouncementCard
              key={announcement.id}
              announcement={announcement}
              editable={editable}
              onDelete={() => deleteAnnouncement.mutate(announcement.id)}
              deletePending={deleteAnnouncement.isPending && deleteAnnouncement.variables === announcement.id}
            />
          ))
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No announcements yet.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}

export function StudentAnnouncementsSection({ courseId }: { courseId: string }) {
  const announcements = useMyCourseAnnouncements(courseId);

  return (
    <div>
      <h2 className="mb-3 text-lg font-medium text-foreground">Announcements</h2>
      <div className="flex flex-col gap-3">
        {announcements.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : announcements.data && announcements.data.length > 0 ? (
          announcements.data.map((announcement) => (
            <AnnouncementCard key={announcement.id} announcement={announcement} editable={false} />
          ))
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No announcements yet.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
