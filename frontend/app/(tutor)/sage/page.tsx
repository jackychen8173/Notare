"use client";

import { useState } from "react";

import { SageChatMessage } from "@/components/sage/SageChatMessage";
import { SageConversationList } from "@/components/sage/SageConversationList";
import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useConfirmSageAction,
  useDeclineSageAction,
  useSageConversation,
  useSageConversations,
  useSendSageMessage,
} from "@/hooks/useSageChat";
import { useIsDemo } from "@/hooks/useIsDemo";
import { errorMessage } from "@/lib/api";

export default function SagePage() {
  const [activeId, setActiveId] = useState<string | null>(null);
  const [draft, setDraft] = useState("");

  const conversations = useSageConversations();
  const conversation = useSageConversation(activeId);
  const sendMessage = useSendSageMessage();
  const isDemo = useIsDemo();
  const confirmAction = useConfirmSageAction();
  const declineAction = useDeclineSageAction();

  function handleSend() {
    if (!draft.trim()) return;
    sendMessage.mutate(
      { conversationId: activeId, message: draft },
      {
        onSuccess: (data) => {
          setActiveId(data.conversationId);
          setDraft("");
        },
      },
    );
  }

  return (
    <div className="flex h-full flex-col gap-4">
      <PageHeader title="Ask Sage" description="Ask Sage to look things up or take an action on your behalf." />
      <div className="flex flex-1 gap-4 overflow-hidden">
        <SageConversationList
          conversations={conversations.data ?? []}
          activeId={activeId}
          onSelect={setActiveId}
          onNew={() => setActiveId(null)}
        />
        <div className="flex flex-1 flex-col gap-3 overflow-y-auto">
          {conversation.isLoading && activeId ? (
            <Skeleton className="h-24 w-full" />
          ) : (
            (conversation.data ?? []).map((message) => (
              <SageChatMessage
                key={message.id}
                message={message}
                onConfirm={() => confirmAction.mutate({ conversationId: activeId as string, messageId: message.id })}
                onDecline={() => declineAction.mutate({ conversationId: activeId as string, messageId: message.id })}
                confirmPending={confirmAction.isPending && confirmAction.variables?.messageId === message.id}
                declinePending={declineAction.isPending && declineAction.variables?.messageId === message.id}
              />
            ))
          )}
        </div>
      </div>
      {isDemo ? (
        <p className="text-sm text-muted-foreground">
          Sage chat is turned off in the demo. With a real account, you can ask Sage to look up students,
          draft feedback, create assignments and more.
        </p>
      ) : null}
      {sendMessage.isError ? <p className="text-sm text-destructive">{errorMessage(sendMessage.error)}</p> : null}
      <div className="flex gap-2">
        <Input
          disabled={isDemo}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") handleSend();
          }}
          placeholder="Ask Sage..."
        />
        <Button onClick={handleSend} disabled={isDemo || sendMessage.isPending}>
          {sendMessage.isPending ? "Sending..." : "Send"}
        </Button>
      </div>
    </div>
  );
}
