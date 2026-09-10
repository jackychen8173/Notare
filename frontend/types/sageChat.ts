export interface PendingAction {
  toolName: string;
  description: string;
  actionStatus: "PENDING" | "CONFIRMED" | "DECLINED";
}

export interface SageMessage {
  id: string;
  role: "USER" | "ASSISTANT" | "TOOL";
  text: string;
  pendingAction: PendingAction | null;
  createdAt: string;
}

export interface SageConversationSummary {
  id: string;
  preview: string;
  updatedAt: string;
}

export interface ChatTurnResponse {
  conversationId: string;
  messages: SageMessage[];
}
