"use client";

import { use } from "react";

import { DiscussionThreadView } from "@/components/discussion/DiscussionThreadView";

export default function DiscussionThreadPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <DiscussionThreadView scope="tutor" threadId={id} />;
}
