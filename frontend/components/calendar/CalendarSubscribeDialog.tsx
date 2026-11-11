"use client";

import { useState } from "react";
import { IconBrandGoogle, IconCalendarPlus, IconCheck, IconCopy } from "@tabler/icons-react";

import { Button, buttonVariants } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { calendarFeedUrl, useCalendarFeed, useResetCalendarFeed } from "@/hooks/useCalendarFeed";

/** Subscribe to your Notare schedule in Google, Apple or Outlook calendar. */
export function CalendarSubscribeDialog() {
  const [open, setOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);
  const feed = useCalendarFeed(open);
  const reset = useResetCalendarFeed();

  const token = feed.data?.token ?? null;
  const url = token ? calendarFeedUrl(token) : null;
  const webcal = url ? url.replace(/^https?:/, "webcal:") : null;

  async function copy() {
    if (!url) return;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard blocked: the link is selectable in the field.
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        setConfirmReset(false);
      }}
    >
      <DialogTrigger
        render={
          <Button variant="outline" size="sm">
            <IconCalendarPlus className="size-4" />
            Add to my calendar
          </Button>
        }
      />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add to your calendar</DialogTitle>
          <DialogDescription>
            Classes, due dates and tutoring sessions show up in Google, Apple or Outlook calendar. Calendar apps check
            for changes every few hours, so something new can take a while to appear there.
          </DialogDescription>
        </DialogHeader>

        {feed.isLoading ? (
          <p className="text-sm text-muted-foreground">Loading…</p>
        ) : !url ? (
          <Button className="self-start" disabled={reset.isPending} onClick={() => reset.mutate()}>
            {reset.isPending ? "Making your link…" : "Make my calendar link"}
          </Button>
        ) : (
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="calendar-feed-url">Your calendar link</Label>
              <div className="flex gap-2">
                <Input
                  id="calendar-feed-url"
                  readOnly
                  value={url}
                  className="font-mono text-xs"
                  onFocus={(event) => event.currentTarget.select()}
                />
                <Button variant="outline" onClick={copy} aria-label="Copy link">
                  {copied ? <IconCheck className="size-4" /> : <IconCopy className="size-4" />}
                  {copied ? "Copied" : "Copy"}
                </Button>
              </div>
            </div>
            <div className="flex flex-wrap gap-2">
              <a
                href={`https://calendar.google.com/calendar/r?cid=${encodeURIComponent(webcal!)}`}
                target="_blank"
                rel="noopener noreferrer"
                className={buttonVariants({ variant: "outline", size: "sm" })}
              >
                <IconBrandGoogle className="size-4" />
                Google Calendar
              </a>
              <a href={webcal!} className={buttonVariants({ variant: "outline", size: "sm" })}>
                Apple or Outlook
              </a>
            </div>
            <p className="text-xs text-muted-foreground">
              Anyone with this link can see your schedule, so don&apos;t share it. If it gets out, reset it: the old
              link stops working and you add the new one.
            </p>
            {confirmReset ? (
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-sm text-foreground">Reset the link? Calendars using the old one stop updating.</span>
                <Button
                  variant="destructive"
                  size="sm"
                  disabled={reset.isPending}
                  onClick={() => reset.mutate(undefined, { onSuccess: () => setConfirmReset(false) })}
                >
                  Reset link
                </Button>
                <Button variant="ghost" size="sm" onClick={() => setConfirmReset(false)}>
                  Cancel
                </Button>
              </div>
            ) : (
              <Button variant="ghost" size="sm" className="self-start" onClick={() => setConfirmReset(true)}>
                Reset link
              </Button>
            )}
          </div>
        )}
        {reset.isError ? <p className="text-xs text-destructive">Couldn&apos;t make the link. Try again.</p> : null}
      </DialogContent>
    </Dialog>
  );
}
