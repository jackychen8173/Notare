import type { UserRole } from "@/types/user";

export type DiscussionVisibility = "PUBLIC" | "PRIVATE";

// Which API surface a discussion screen talks to. The same components render both sides.
export type DiscussionScope = "tutor" | "student";

export interface DiscussionAuthor {
  // null when the author is anonymous and the viewer is a classmate - the server never sends the identity.
  id: string | null;
  name: string;
  role: UserRole;
  anonymous: boolean;
  mine: boolean;
}

export interface DiscussionThreadSummary {
  id: string;
  courseId: string;
  title: string;
  visibility: DiscussionVisibility;
  pinned: boolean;
  locked: boolean;
  author: DiscussionAuthor;
  replyCount: number;
  createdAt: string;
  lastActivityAt: string;
  unread: boolean;
}

export interface DiscussionPost {
  id: string;
  threadId: string;
  body: string;
  author: DiscussionAuthor;
  createdAt: string;
  editedAt: string | null;
}

export interface DiscussionThreadDetail {
  id: string;
  courseId: string;
  title: string;
  body: string;
  visibility: DiscussionVisibility;
  pinned: boolean;
  locked: boolean;
  author: DiscussionAuthor;
  createdAt: string;
  editedAt: string | null;
  lastActivityAt: string;
  posts: DiscussionPost[];
}

export interface CreateThreadInput {
  title: string;
  body: string;
  visibility?: DiscussionVisibility;
  anonymous?: boolean;
}
