import type { UserRole } from "@/types/user";

export type ProblemReportCategory = "BUG" | "CONFUSING" | "SUGGESTION" | "OTHER";
export type ProblemReportStatus = "OPEN" | "RESOLVED";

export const PROBLEM_REPORT_CATEGORIES: { value: ProblemReportCategory; label: string }[] = [
  { value: "BUG", label: "Something's broken" },
  { value: "CONFUSING", label: "Something's confusing" },
  { value: "SUGGESTION", label: "Suggestion" },
  { value: "OTHER", label: "Other" },
];

export interface ProblemReport {
  id: string;
  reporterId: string;
  reporterName: string;
  reporterEmail: string;
  reporterRole: UserRole;
  reporterDemo: boolean;
  category: ProblemReportCategory;
  message: string;
  pageUrl: string | null;
  userAgent: string | null;
  status: ProblemReportStatus;
  createdAt: string;
  resolvedAt: string | null;
}
