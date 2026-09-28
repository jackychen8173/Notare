"use client";

import { useState } from "react";
import { IconMessageReport } from "@tabler/icons-react";

import { EmptyState } from "@/components/layout/EmptyState";
import { PageHeader } from "@/components/layout/PageHeader";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminProblemReports, useSetProblemReportStatus } from "@/hooks/useProblemReports";
import { PROBLEM_REPORT_CATEGORIES, type ProblemReport, type ProblemReportStatus } from "@/types/problemReport";

const FILTERS: { value: ProblemReportStatus | null; label: string }[] = [
  { value: "OPEN", label: "Open" },
  { value: "RESOLVED", label: "Resolved" },
  { value: null, label: "All" },
];

const ROLE_LABEL = { TUTOR: "Tutor", STUDENT: "Student", ADMIN: "Admin" } as const;

function categoryLabel(report: ProblemReport) {
  return PROBLEM_REPORT_CATEGORIES.find((c) => c.value === report.category)?.label ?? report.category;
}

function ReportCard({ report }: { report: ProblemReport }) {
  const setStatus = useSetProblemReportStatus();
  const resolved = report.status === "RESOLVED";

  return (
    <article className="flex flex-col gap-3 rounded-card border border-border bg-card p-5 shadow-card">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant={report.category === "BUG" ? "destructive" : "outline"}>{categoryLabel(report)}</Badge>
          <span className="text-sm font-medium text-foreground">{report.reporterName}</span>
          <span className="text-sm text-muted-foreground">
            {ROLE_LABEL[report.reporterRole]}
            {report.reporterDemo ? " · demo" : ` · ${report.reporterEmail}`}
          </span>
        </div>
        <span className="text-xs text-muted-foreground">{new Date(report.createdAt).toLocaleString()}</span>
      </div>
      <p className="text-sm whitespace-pre-wrap text-foreground">{report.message}</p>
      <div className="flex flex-col gap-0.5 text-xs text-muted-foreground">
        {report.pageUrl ? (
          <p className="truncate">
            Page:{" "}
            <a href={report.pageUrl} target="_blank" rel="noreferrer" className="text-primary hover:underline">
              {report.pageUrl}
            </a>
          </p>
        ) : null}
        {report.userAgent ? <p className="truncate">Browser: {report.userAgent}</p> : null}
        {report.resolvedAt ? <p>Resolved {new Date(report.resolvedAt).toLocaleString()}</p> : null}
      </div>
      <div>
        <Button
          variant={resolved ? "outline" : "default"}
          size="sm"
          disabled={setStatus.isPending}
          onClick={() => setStatus.mutate({ id: report.id, resolved: !resolved })}
        >
          {resolved ? "Reopen" : "Mark resolved"}
        </Button>
      </div>
    </article>
  );
}

export default function AdminReportsPage() {
  const [status, setStatus] = useState<ProblemReportStatus | null>("OPEN");
  const reports = useAdminProblemReports(status);

  return (
    <>
      <PageHeader title="Reports" description="Problems and suggestions sent from the Report a problem menu." />

      <div className="mb-6 inline-flex rounded-lg bg-muted p-1">
        {FILTERS.map((filter) => (
          <button
            key={filter.label}
            type="button"
            onClick={() => setStatus(filter.value)}
            className={
              status === filter.value
                ? "rounded-md bg-card px-3 py-1 text-sm font-medium text-foreground shadow-xs"
                : "rounded-md px-3 py-1 text-sm font-medium text-muted-foreground hover:text-foreground"
            }
          >
            {filter.label}
          </button>
        ))}
      </div>

      {reports.isLoading ? (
        <div className="flex flex-col gap-3">
          <Skeleton className="h-32 w-full rounded-card" />
          <Skeleton className="h-32 w-full rounded-card" />
        </div>
      ) : reports.data && reports.data.length > 0 ? (
        <div className="flex flex-col gap-3">
          {reports.data.map((report) => (
            <ReportCard key={report.id} report={report} />
          ))}
        </div>
      ) : (
        <EmptyState
          icon={IconMessageReport}
          title={status === "OPEN" ? "No open reports" : "No reports"}
          description="Reports people send from the account menu show up here."
        />
      )}
    </>
  );
}
