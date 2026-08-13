import type { SageFeedback } from "@/types/sage";

interface SageFeedbackBlockProps {
  feedbackJson: string;
}

const sections: { key: keyof SageFeedback; label: string }[] = [
  { key: "correctness", label: "Correctness" },
  { key: "style", label: "Style" },
  { key: "suggestions", label: "Suggestions" },
  { key: "encouragement", label: "Encouragement" },
];

export function SageFeedbackBlock({ feedbackJson }: SageFeedbackBlockProps) {
  let feedback: SageFeedback | null = null;
  try {
    feedback = JSON.parse(feedbackJson) as SageFeedback;
  } catch {
    feedback = null;
  }

  if (!feedback) {
    return <p className="text-sm text-destructive">Sage feedback couldn&apos;t be read.</p>;
  }

  const parsedFeedback = feedback;

  return (
    <div className="flex flex-col gap-3 rounded-card border-hairline border-sage-border bg-sage-surface p-4">
      <p className="text-xs font-medium text-sage-text">Sage feedback</p>
      {sections.map(({ key, label }) => (
        <div key={key}>
          <p className="text-xs font-medium text-sage-text/80">{label}</p>
          <p className="text-sm text-sage-text">{parsedFeedback[key]}</p>
        </div>
      ))}
    </div>
  );
}
