import { classHighlighter, highlightCode } from "@lezer/highlight";
import { parser } from "@lezer/java";

export interface HighlightToken {
  text: string;
  className: string;
}

/** Java source split into lines of highlighted tokens (classes are styled in globals.css). */
export function highlightJavaLines(code: string): HighlightToken[][] {
  const lines: HighlightToken[][] = [[]];
  highlightCode(
    code,
    parser.parse(code),
    classHighlighter,
    (text, className) => lines[lines.length - 1].push({ text, className }),
    () => lines.push([]),
  );
  return lines;
}
