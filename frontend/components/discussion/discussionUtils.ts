import axios from "axios";

import type { DiscussionAuthor } from "@/types/discussion";

export function authorLabel(author: DiscussionAuthor): string {
  const name = author.mine ? "You" : author.name;
  // author.id is only present on an anonymous post when the viewer is allowed to see who wrote it
  // (the tutor, or the author themselves) - so this note only ever shows to those two.
  if (author.anonymous && author.id !== null) {
    return author.mine ? `${name} (anonymous to classmates)` : `${name} (posted anonymously)`;
  }
  return author.role === "TUTOR" && !author.mine ? `${name} · Tutor` : name;
}

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? "Something went wrong. Try again.";
  }
  return "Something went wrong. Try again.";
}
