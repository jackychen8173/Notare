"use client";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { SageConversationSummary } from "@/types/sageChat";

interface SageConversationListProps {
  conversations: SageConversationSummary[];
  activeId: string | null;
  onSelect: (id: string) => void;
  onNew: () => void;
}

export function SageConversationList({ conversations, activeId, onSelect, onNew }: SageConversationListProps) {
  return (
    <div className="flex w-64 shrink-0 flex-col gap-2 border-r-hairline border-border pr-4">
      <Button variant="outline" size="sm" onClick={onNew}>
        New conversation
      </Button>
      <div className="flex flex-col gap-1">
        {conversations.map((conversation) => (
          <button
            key={conversation.id}
            onClick={() => onSelect(conversation.id)}
            className={cn(
              "truncate rounded-lg px-3 py-2 text-left text-sm transition-colors",
              activeId === conversation.id
                ? "bg-sidebar-accent text-sidebar-accent-foreground"
                : "text-muted-foreground hover:bg-sidebar-accent/50",
            )}
          >
            {conversation.preview}
          </button>
        ))}
      </div>
    </div>
  );
}
