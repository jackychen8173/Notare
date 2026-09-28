"use client";

import { useState } from "react";
import { IconCircleCheck } from "@tabler/icons-react";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useCreateProblemReport } from "@/hooks/useProblemReports";
import { errorMessage } from "@/lib/api";
import { cn } from "@/lib/utils";
import { PROBLEM_REPORT_CATEGORIES, type ProblemReportCategory } from "@/types/problemReport";

const MAX_MESSAGE_LENGTH = 5000;

/** "Report a problem": saved on the backend and triaged in the admin portal's Reports page. */
export function ReportProblemDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const createReport = useCreateProblemReport();
  const [category, setCategory] = useState<ProblemReportCategory>("BUG");
  const [message, setMessage] = useState("");

  function handleOpenChange(nextOpen: boolean) {
    onOpenChange(nextOpen);
    if (!nextOpen) {
      setCategory("BUG");
      setMessage("");
      createReport.reset();
    }
  }

  function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!message.trim()) return;
    createReport.mutate({ category, message: message.trim(), pageUrl: window.location.href });
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        {createReport.isSuccess ? (
          <div className="flex flex-col items-center gap-3 py-4 text-center">
            <IconCircleCheck className="size-10 text-primary" stroke={1.5} />
            <DialogTitle>Thanks for letting us know</DialogTitle>
            <DialogDescription>Your report was sent. We read every one.</DialogDescription>
            <Button className="mt-2" onClick={() => handleOpenChange(false)}>
              Done
            </Button>
          </div>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Report a problem</DialogTitle>
              <DialogDescription>
                Tell us what happened. We&apos;ll include the page you&apos;re on so we can find it.
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={onSubmit} className="flex flex-col gap-4">
              <div className="flex flex-wrap gap-1.5" role="radiogroup" aria-label="What kind of problem?">
                {PROBLEM_REPORT_CATEGORIES.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    role="radio"
                    aria-checked={category === option.value}
                    onClick={() => setCategory(option.value)}
                    className={cn(
                      "h-8 rounded-full border px-3 text-xs font-medium transition-colors outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
                      category === option.value
                        ? "border-primary bg-primary text-primary-foreground"
                        : "border-border bg-card text-muted-foreground hover:text-foreground",
                    )}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="report-message">What happened?</Label>
                <Textarea
                  id="report-message"
                  rows={5}
                  maxLength={MAX_MESSAGE_LENGTH}
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder="What were you trying to do, and what went wrong?"
                />
              </div>
              {createReport.isError ? (
                <p className="text-xs text-destructive">{errorMessage(createReport.error)}</p>
              ) : null}
              <DialogFooter>
                <Button type="submit" disabled={!message.trim() || createReport.isPending}>
                  {createReport.isPending ? "Sending..." : "Send report"}
                </Button>
              </DialogFooter>
            </form>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
