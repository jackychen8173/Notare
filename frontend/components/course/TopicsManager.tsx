"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

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
import { Skeleton } from "@/components/ui/skeleton";
import { useCreateTopic, useDeleteTopic, useRenameTopic, useTopics } from "@/hooks/useTopics";
import type { Topic } from "@/types/topic";

const topicSchema = z.object({ name: z.string().min(1, "Name is required") });
type TopicValues = z.infer<typeof topicSchema>;

function NewTopicDialog({ courseId }: { courseId: string }) {
  const [open, setOpen] = useState(false);
  const createTopic = useCreateTopic(courseId);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TopicValues>({ resolver: zodResolver(topicSchema) });

  function onSubmit(values: TopicValues) {
    createTopic.mutate(values.name, {
      onSuccess: () => {
        reset();
        setOpen(false);
      },
    });
  }

  return (
    <Dialog open={open} onOpenChange={(next) => { setOpen(next); if (!next) reset(); }}>
      <DialogTrigger render={<Button variant="outline" size="sm">New topic</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New topic</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="topic-name">Name</Label>
            <Input id="topic-name" {...register("name")} />
            {errors.name ? <p className="text-xs text-destructive">{errors.name.message}</p> : null}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={createTopic.isPending}>
              {createTopic.isPending ? "Creating..." : "Create topic"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function RenameTopicDialog({ courseId, topic }: { courseId: string; topic: Topic }) {
  const [open, setOpen] = useState(false);
  const renameTopic = useRenameTopic(courseId);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TopicValues>({ resolver: zodResolver(topicSchema), values: { name: topic.name } });

  function onSubmit(values: TopicValues) {
    renameTopic.mutate({ id: topic.id, name: values.name }, { onSuccess: () => setOpen(false) });
  }

  return (
    <Dialog open={open} onOpenChange={(next) => { setOpen(next); if (!next) reset(); }}>
      <DialogTrigger render={<Button variant="outline" size="sm">Rename</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Rename topic</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="rename-topic">Name</Label>
            <Input id="rename-topic" {...register("name")} />
            {errors.name ? <p className="text-xs text-destructive">{errors.name.message}</p> : null}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={renameTopic.isPending}>
              {renameTopic.isPending ? "Saving..." : "Save"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function TopicsManager({ courseId, editable }: { courseId: string; editable: boolean }) {
  const topics = useTopics(courseId);
  const deleteTopic = useDeleteTopic(courseId);

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-medium text-foreground">Topics</h2>
        {editable ? <NewTopicDialog courseId={courseId} /> : null}
      </div>
      {topics.isLoading ? (
        <Skeleton className="h-16 w-full" />
      ) : topics.data && topics.data.length > 0 ? (
        <div className="flex flex-col gap-2">
          {topics.data.map((topic) => (
            <Card key={topic.id}>
              <CardContent className="flex items-center justify-between gap-4">
                <p className="text-sm font-medium text-foreground">{topic.name}</p>
                {editable ? (
                  <div className="flex gap-2">
                    <RenameTopicDialog courseId={courseId} topic={topic} />
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={deleteTopic.isPending && deleteTopic.variables === topic.id}
                      onClick={() => deleteTopic.mutate(topic.id)}
                    >
                      Delete
                    </Button>
                  </div>
                ) : null}
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <Card>
          <CardContent>
            <p className="text-sm text-muted-foreground">No topics yet.</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
