"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { authorLabel, errorMessage } from "@/components/discussion/discussionUtils";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import {
  useDeletePost,
  useDeleteThread,
  useDiscussionThread,
  useMakeThreadPublic,
  useModerateThread,
  useReplyToThread,
  useUpdatePost,
  useUpdateThread,
} from "@/hooks/useDiscussions";
import type { DiscussionPost, DiscussionScope, DiscussionThreadDetail } from "@/types/discussion";

function timestamp(createdAt: string, editedAt: string | null) {
  const created = new Date(createdAt).toLocaleString();
  return editedAt ? `${created} · edited` : created;
}

function TutorControls({ thread }: { thread: DiscussionThreadDetail }) {
  const router = useRouter();
  const moderate = useModerateThread(thread);
  const makePublic = useMakeThreadPublic(thread);
  const deleteThread = useDeleteThread("tutor", thread.courseId);

  function onMakePublic() {
    const ok = window.confirm(
      "Make this thread visible to the whole class? The student's posts in it will be shown to classmates as Anonymous. This can't be undone.",
    );
    if (ok) makePublic.mutate();
  }

  function onDelete() {
    if (window.confirm("Delete this thread and all its replies?")) {
      deleteThread.mutate(thread.id, { onSuccess: () => router.push(`/courses/${thread.courseId}`) });
    }
  }

  const error = moderate.error ?? makePublic.error ?? deleteThread.error;

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={moderate.isPending}
          onClick={() => moderate.mutate({ pinned: !thread.pinned })}
        >
          {thread.pinned ? "Unpin" : "Pin"}
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={moderate.isPending}
          onClick={() => moderate.mutate({ locked: !thread.locked })}
        >
          {thread.locked ? "Unlock" : "Lock"}
        </Button>
        {thread.visibility === "PRIVATE" ? (
          <Button variant="outline" size="sm" disabled={makePublic.isPending} onClick={onMakePublic}>
            {makePublic.isPending ? "Making public..." : "Make public"}
          </Button>
        ) : null}
        <Button variant="outline" size="sm" disabled={deleteThread.isPending} onClick={onDelete}>
          Delete thread
        </Button>
      </div>
      {error ? <p className="text-xs text-destructive">{errorMessage(error)}</p> : null}
    </div>
  );
}

function ThreadBody({ scope, thread }: { scope: DiscussionScope; thread: DiscussionThreadDetail }) {
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState(thread.title);
  const [body, setBody] = useState(thread.body);
  const updateThread = useUpdateThread(scope, thread);
  const deleteThread = useDeleteThread(scope, thread.courseId);

  function startEditing() {
    setTitle(thread.title);
    setBody(thread.body);
    updateThread.reset();
    setEditing(true);
  }

  function onSave() {
    if (!title.trim() || !body.trim()) return;
    updateThread.mutate({ title: title.trim(), body }, { onSuccess: () => setEditing(false) });
  }

  function onStudentDelete() {
    if (window.confirm("Delete this post?")) {
      deleteThread.mutate(thread.id, { onSuccess: () => router.push(`/student/courses/${thread.courseId}`) });
    }
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        {editing ? (
          <>
            <Input maxLength={255} value={title} onChange={(event) => setTitle(event.target.value)} />
            <Textarea rows={5} value={body} onChange={(event) => setBody(event.target.value)} />
            {updateThread.isError ? (
              <p className="text-xs text-destructive">{errorMessage(updateThread.error)}</p>
            ) : null}
            <div className="flex gap-2 self-end">
              <Button variant="outline" size="sm" onClick={() => setEditing(false)}>
                Cancel
              </Button>
              <Button size="sm" disabled={updateThread.isPending || !title.trim() || !body.trim()} onClick={onSave}>
                {updateThread.isPending ? "Saving..." : "Save"}
              </Button>
            </div>
          </>
        ) : (
          <>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-medium text-foreground">{thread.title}</h1>
              {thread.pinned ? <Badge variant="secondary">Pinned</Badge> : null}
              {thread.visibility === "PRIVATE" ? (
                <Badge variant="outline">{scope === "tutor" ? "Private" : "Private · only you and your tutor"}</Badge>
              ) : null}
              {thread.locked ? <Badge variant="outline">Locked</Badge> : null}
            </div>
            <p className="text-xs text-muted-foreground">
              {authorLabel(thread.author)} · {timestamp(thread.createdAt, thread.editedAt)}
            </p>
            <p className="whitespace-pre-wrap text-sm text-foreground">{thread.body}</p>
            {thread.author.mine ? (
              <div className="flex gap-2 self-end">
                <Button variant="outline" size="sm" onClick={startEditing}>
                  Edit
                </Button>
                {/* Students can only remove their own thread before anyone replies - replies would vanish with it. */}
                {scope === "student" && thread.posts.length === 0 ? (
                  <Button variant="outline" size="sm" disabled={deleteThread.isPending} onClick={onStudentDelete}>
                    Delete
                  </Button>
                ) : null}
              </div>
            ) : null}
            {deleteThread.isError ? (
              <p className="text-xs text-destructive">{errorMessage(deleteThread.error)}</p>
            ) : null}
          </>
        )}
      </CardContent>
    </Card>
  );
}

function PostItem({
  scope,
  thread,
  post,
}: {
  scope: DiscussionScope;
  thread: DiscussionThreadDetail;
  post: DiscussionPost;
}) {
  const [editing, setEditing] = useState(false);
  const [body, setBody] = useState(post.body);
  const updatePost = useUpdatePost(scope, thread);
  const deletePost = useDeletePost(scope, thread);
  // The tutor moderates every reply in their course; students manage only their own.
  const canDelete = post.author.mine || scope === "tutor";

  function onSave() {
    if (!body.trim()) return;
    updatePost.mutate({ postId: post.id, body }, { onSuccess: () => setEditing(false) });
  }

  function onDelete() {
    if (window.confirm("Delete this reply?")) deletePost.mutate(post.id);
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-2">
        <p className="text-xs text-muted-foreground">
          {authorLabel(post.author)} · {timestamp(post.createdAt, post.editedAt)}
        </p>
        {editing ? (
          <>
            <Textarea rows={3} value={body} onChange={(event) => setBody(event.target.value)} />
            {updatePost.isError ? <p className="text-xs text-destructive">{errorMessage(updatePost.error)}</p> : null}
            <div className="flex gap-2 self-end">
              <Button variant="outline" size="sm" onClick={() => setEditing(false)}>
                Cancel
              </Button>
              <Button size="sm" disabled={updatePost.isPending || !body.trim()} onClick={onSave}>
                {updatePost.isPending ? "Saving..." : "Save"}
              </Button>
            </div>
          </>
        ) : (
          <>
            <p className="whitespace-pre-wrap text-sm text-foreground">{post.body}</p>
            {canDelete ? (
              <div className="flex gap-2 self-end">
                {post.author.mine ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setBody(post.body);
                      updatePost.reset();
                      setEditing(true);
                    }}
                  >
                    Edit
                  </Button>
                ) : null}
                {canDelete ? (
                  <Button variant="outline" size="sm" disabled={deletePost.isPending} onClick={onDelete}>
                    Delete
                  </Button>
                ) : null}
              </div>
            ) : null}
            {deletePost.isError ? <p className="text-xs text-destructive">{errorMessage(deletePost.error)}</p> : null}
          </>
        )}
      </CardContent>
    </Card>
  );
}

function ReplyForm({ scope, thread }: { scope: DiscussionScope; thread: DiscussionThreadDetail }) {
  const [body, setBody] = useState("");
  const [anonymous, setAnonymous] = useState(false);
  const reply = useReplyToThread(scope, thread);

  if (thread.locked && scope === "student") {
    return <p className="text-sm text-muted-foreground">This thread is locked — no new replies.</p>;
  }

  // Anonymity only means something in front of classmates, never in a private thread.
  const canPostAnonymously = scope === "student" && thread.visibility === "PUBLIC";

  function onSubmit() {
    if (!body.trim()) return;
    reply.mutate(
      { body, anonymous: canPostAnonymously && anonymous },
      {
        onSuccess: () => {
          setBody("");
          setAnonymous(false);
        },
      },
    );
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <Textarea rows={3} placeholder="Write a reply..." value={body} onChange={(event) => setBody(event.target.value)} />
        {reply.isError ? <p className="text-xs text-destructive">{errorMessage(reply.error)}</p> : null}
        <div className="flex flex-wrap items-center justify-between gap-2">
          {canPostAnonymously ? (
            <label className="flex items-center gap-2 text-sm text-foreground">
              <input type="checkbox" checked={anonymous} onChange={(event) => setAnonymous(event.target.checked)} />
              Anonymous to classmates
            </label>
          ) : (
            <span />
          )}
          <Button size="sm" disabled={reply.isPending || !body.trim()} onClick={onSubmit}>
            {reply.isPending ? "Posting..." : "Reply"}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export function DiscussionThreadView({ scope, threadId }: { scope: DiscussionScope; threadId: string }) {
  const thread = useDiscussionThread(scope, threadId);

  if (thread.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (!thread.data) {
    return <p className="text-sm text-muted-foreground">Thread not found.</p>;
  }

  const data = thread.data;
  const courseHref = scope === "tutor" ? `/courses/${data.courseId}` : `/student/courses/${data.courseId}`;

  return (
    <div className="flex flex-col gap-4">
      <Link href={courseHref} className="text-sm text-muted-foreground hover:text-foreground">
        ← Back to course
      </Link>
      {/* Keyed by edit time so an in-progress edit form resets to fresh values after a save lands. */}
      <ThreadBody key={`${data.id}-${data.editedAt}`} scope={scope} thread={data} />
      {scope === "tutor" ? <TutorControls thread={data} /> : null}

      <h2 className="text-lg font-medium text-foreground">
        {data.posts.length} {data.posts.length === 1 ? "reply" : "replies"}
      </h2>
      {data.posts.map((post) => (
        <PostItem key={`${post.id}-${post.editedAt}`} scope={scope} thread={data} post={post} />
      ))}
      <ReplyForm scope={scope} thread={data} />
    </div>
  );
}
