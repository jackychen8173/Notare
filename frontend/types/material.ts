import type { StoredFileInfo } from "@/types/file";

export type MaterialType = "LINK" | "GOOGLE_DOC" | "GOOGLE_SLIDES" | "PDF" | "FILE";

export interface Material {
  id: string;
  courseId: string;
  topicId: string | null;
  topicName: string | null;
  title: string;
  description: string | null;
  url: string | null;
  type: MaterialType;
  createdAt: string;
  /** Set for an uploaded file (type FILE). */
  file: StoredFileInfo | null;
}
