"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import type { SageMessage } from "@/types/sageChat";

interface SageChatMessageProps {
  message: SageMessage;
  onConfirm: () => void;
  onDecline: () => void;
  confirmPending: boolean;
  declinePending: boolean;
}

export function SageChatMessage({ message, onConfirm, onDecline, confirmPending, declinePending }: SageChatMessageProps) {
  const isUser = message.role === "USER";

  if (message.role === "TOOL") {
    return null;
  }

  return (
    <div className={`flex ${isUser ? "justify-end" : "justify-start"}`}>
      <div className={`max-w-[80%] rounded-card px-4 py-2.5 text-sm ${isUser ? "bg-primary text-primary-foreground" : "bg-muted text-foreground"}`}>
        {message.text ? <p className="whitespace-pre-wrap">{message.text}</p> : null}
        {message.pendingAction ? (
          <Card className="mt-2">
            <CardContent className="flex flex-col gap-2">
              <p className="whitespace-pre-wrap text-sm text-foreground">{message.pendingAction.description}</p>
              {message.pendingAction.actionStatus === "PENDING" ? (
                <div className="flex gap-2">
                  <Button size="sm" disabled={confirmPending} onClick={onConfirm}>
                    {confirmPending ? "Confirming..." : "Confirm"}
                  </Button>
                  <Button size="sm" variant="outline" disabled={declinePending} onClick={onDecline}>
                    {declinePending ? "Declining..." : "Decline"}
                  </Button>
                </div>
              ) : (
                <p className="text-xs text-muted-foreground">
                  {message.pendingAction.actionStatus === "CONFIRMED" ? "Confirmed." : "Declined."}
                </p>
              )}
            </CardContent>
          </Card>
        ) : null}
      </div>
    </div>
  );
}
