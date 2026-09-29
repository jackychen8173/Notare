"use client";

import CodeMirror, { EditorView } from "@uiw/react-codemirror";
import { java } from "@codemirror/lang-java";
import { syntaxHighlighting } from "@codemirror/language";
import { classHighlighter } from "@lezer/highlight";
import { useTheme } from "next-themes";

import { cn } from "@/lib/utils";

// Themed against the design tokens (app/globals.css) rather than a canned CodeMirror theme, so the
// editor matches the rest of the app in both light and dark mode. Syntax colors are the same tok-*
// classes read-only code uses (classHighlighter + globals.css), so neither theme ever shows green,
// which is reserved for Sage.
const editorStyles = {
  "&": {
    backgroundColor: "var(--background)",
    color: "var(--foreground)",
    fontSize: "0.8rem",
  },
  ".cm-content": {
    fontFamily: "var(--font-mono)",
    fontVariantLigatures: "none",
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
  /** Any CSS height; "100%" fills a sized parent (full-screen editors). */
  height?: string;
  className?: string;
}

export function CodeEditor({ value, onChange, readOnly, height = "320px", className }: CodeEditorProps) {
  const dark = useTheme().resolvedTheme === "dark";

  return (
    <div className={cn("overflow-hidden rounded-card border border-border", className)}>
      <CodeMirror
        value={value}
        onChange={onChange}
        theme={dark ? darkEditorTheme : lightEditorTheme}
        extensions={[java(), syntaxHighlighting(classHighlighter)]}
        readOnly={readOnly}
        basicSetup={{ tabSize: 4 }}
        height={height}
      />
    </div>
  );
}
