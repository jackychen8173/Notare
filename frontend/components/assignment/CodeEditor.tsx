"use client";

import CodeMirror, { EditorView } from "@uiw/react-codemirror";
import { java } from "@codemirror/lang-java";

// Themed against the existing design tokens (app/globals.css) rather than a canned CodeMirror
// theme, so the editor matches the house system instead of looking like a foreign widget - no
// shadows, hairline border (on the wrapping div below), house monospace font.
const notareEditorTheme = EditorView.theme(
  {
    "&": {
      backgroundColor: "var(--background)",
      color: "var(--foreground)",
      fontSize: "0.8rem",
    },
    ".cm-content": {
      fontFamily: "var(--font-mono)",
      caretColor: "var(--foreground)",
    },
    ".cm-gutters": {
      backgroundColor: "var(--muted)",
      color: "var(--muted-foreground)",
      border: "none",
    },
    ".cm-activeLine": {
      backgroundColor: "var(--muted)",
    },
    ".cm-activeLineGutter": {
      backgroundColor: "var(--muted)",
    },
    "&.cm-focused": {
      outline: "none",
    },
    ".cm-selectionBackground, ::selection": {
      backgroundColor: "var(--accent) !important",
    },
  },
  { dark: false },
);

interface CodeEditorProps {
  value: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
}

export function CodeEditor({ value, onChange, readOnly }: CodeEditorProps) {
  return (
    <div className="overflow-hidden rounded-card border-hairline border-border">
      <CodeMirror
        value={value}
        onChange={onChange}
        extensions={[java(), notareEditorTheme]}
        readOnly={readOnly}
        basicSetup={{ tabSize: 4 }}
        height="320px"
      />
    </div>
  );
}
