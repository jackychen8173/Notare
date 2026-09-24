"use client";

import { useState } from "react";
import Link from "next/link";

import { authorLabel, errorMessage } from "@/components/discussion/discussionUtils";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useCreateDiscussionThread, useDiscussionThreads } from "@/hooks/useDiscussions";
import type { DiscussionScope, DiscussionThreadSummary, DiscussionVisibility } from "@/types/discussion";

function threadHref(scope: DiscussionScope, threadId: string) {
  return scope === "tutor" ? `/discussions/${threadId}` : `/student/discussions/${threadId}`;
}

function NewThreadDialog({ scope, courseId }: { scope: DiscussionScope; courseId: string }) {
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [visibility, setVisibility] = useState<DiscussionVisibility>("PUBLIC");
  const [anonymous, setAnonymous] = useState(false);
  const createThread = useCreateDiscussionThread(scope, courseId);

  function reset() {
    setTitle("");
    setBody("");
    setVisibility("PUBLIC");
    setAnonymous(false);
    createThread.reset();
  }

  function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!title.trim() || !body.trim()) return;
    createThread.mutate(
      scope === "student"
        ? { title: title.trim(), body, visibility, anonymous: visibility === "PUBLIC" && anonymous }
        : { title: title.trim(), body },
      {
        onSuccess: () => {
          reset();
          setOpen(false);
        },
      },
    );
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (!nextOpen) reset();
      }}
    >
      <DialogTrigger render={<Button variant="outline">New post</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New post</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="thread-title">Title</Label>
            <Input id="thread-title" maxLength={255} value={title} onChange={(event) => setTitle(event.target.value)} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="thread-body">Message</Label>
            <Textarea id="thread-body" rows={5} value={body} onChange={(event) => setBody(event.target.value)} />
          </div>
          {scope === "student" ? (
            <>
              <div className="flex flex-col gap-2">
                <Label>Who can see this</Label>
                <RadioGroup
                  value={visibility}
                  onValueChange={(value) => setVisibility(value as DiscussionVisibility)}
                  className="gap-2"
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="PUBLIC" id="thread-visibility-public" />
                    <label htmlFor="thread-visibility-public" className="text-sm text-foreground">
                      Whole class
                    </label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="PRIVATE" id="thread-visibility-private" />
                    <label htmlFor="thread-visibility-private" className="text-sm text-foreground">
                      Only my tutor
                    </label>
                  </div>
                </RadioGroup>
              </div>
              {visibility === "PUBLIC" ? (
                <label className="flex items-center gap-2 text-sm text-foreground">
                  <input
                    type="checkbox"
                    checked={anonymous}
                    onChange={(event) => setAnonymous(event.target.checked)}
                  />
                  Post anonymously to classmates (your tutor still sees your name)
                </label>
              ) : null}
            </>
          ) : null}
          {createThread.isError ? (
            <p className="text-xs text-destructive">{errorMessage(createThread.error)}</p>
          ) : null}
          <DialogFooter>
            <Button type="submit" disabled={createThread.isPending || !title.trim() || !body.trim()}>
              {createThread.isPending ? "Posting..." : "Post"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ThreadRow({ scope, thread }: { scope: DiscussionScope; thread: DiscussionThreadSummary }) {
  return (
    <Link href={threadHref(scope, thread.id)} className="block">
      <Card className="transition-colors hover:bg-muted/50">
        <CardContent className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              {thread.unread ? <span aria-label="Unread" className="size-2 shrink-0 rounded-full bg-primary" /> : null}
              <p className={`truncate text-sm text-foreground ${thread.unread ? "font-medium" : ""}`}>{thread.title}</p>
              {thread.pinned ? <Badge variant="secondary">Pinned</Badge> : null}
              {thread.visibility === "PRIVATE" ? <Badge variant="outline">Private</Badge> : null}
              {thread.locked ? <Badge variant="outline">Locked</Badge> : null}
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              {authorLabel(thread.author)} · {new Date(thread.lastActivityAt).toLocaleString()}
            </p>
          </div>
          <p className="shrink-0 text-xs text-muted-foreground">
            {thread.replyCount} {thread.replyCount === 1 ? "reply" : "replies"}
          </p>
        </CardContent>
      </Card>
    </Link>
  );
}

export function DiscussionsSection({
  scope,
  courseId,
  archived = false,
}: {
  scope: DiscussionScope;
  courseId: string;
  archived?: boolean;
}) {
  const threads = useDiscussionThreads(scope, courseId);
  const unreadCount = threads.data?.filter((thread) => thread.unread).length ?? 0;

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-medium text-foreground">Discussions</h2>
          {unreadCount > 0 ? <Badge>{unreadCount} new</Badge> : null}
        </div>
        {!archived ? <NewThreadDialog scope={scope} courseId={courseId} /> : null}
      </div>
      {threads.isLoading ? (
        <Skeleton className="h-16 w-full" />
      ) : threads.data && threads.data.length > 0 ? (
        <div className="flex flex-col gap-2">
          {threads.data.map((thread) => (
            <ThreadRow key={thread.id} scope={scope} thread={thread} />
          ))}
        </div>
      ) : (
        <Card>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              {scope === "student"
                ? "No posts yet. Ask the class a question, or send one privately to your tutor."
                : "No posts yet."}
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
