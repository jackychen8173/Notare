"use client";

import CodeMirror, { EditorView } from "@uiw/react-codemirror";
import { java } from "@codemirror/lang-java";
import { useTheme } from "next-themes";

// Themed against the design tokens (app/globals.css) rather than a canned CodeMirror theme, so the
// editor matches the rest of the app in both light and dark mode. Syntax colors come from
// CodeMirror's own light/dark highlight styles (the `theme` prop below).
const editorStyles = {
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
};

const lightEditorTheme = EditorView.theme(editorStyles, { dark: false });
const darkEditorTheme = EditorView.theme(editorStyles, { dark: true });

interface CodeEditorProps {
  value: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
}

export function CodeEditor({ value, onChange, readOnly }: CodeEditorProps) {
  const dark = useTheme().resolvedTheme === "dark";

  return (
    <div className="overflow-hidden rounded-card border border-border">
      <CodeMirror
        value={value}
        onChange={onChange}
        theme={dark ? "dark" : "light"}
        extensions={[java(), dark ? darkEditorTheme : lightEditorTheme]}
        readOnly={readOnly}
        basicSetup={{ tabSize: 4 }}
        height="320px"
      />
    </div>
  );
}
