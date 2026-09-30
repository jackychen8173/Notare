export type DiffLine =
  | { kind: "same"; text: string; newLine: number }
  | { kind: "added"; text: string; newLine: number }
  | { kind: "removed"; text: string };

/**
 * Line diff of two versions (longest common subsequence). Submissions are short, so the O(n*m)
 * table is fine; above ~2,000 lines on both sides it falls back to "everything changed".
 */
export function diffLines(before: string, after: string): DiffLine[] {
  const a = before.split("\n");
  const b = after.split("\n");
  if (a.length * b.length > 4_000_000) {
    return [
      ...a.map((text) => ({ kind: "removed" as const, text })),
      ...b.map((text, i) => ({ kind: "added" as const, text, newLine: i + 1 })),
    ];
  }

  const lcs: number[][] = Array.from({ length: a.length + 1 }, () => new Array(b.length + 1).fill(0));
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      lcs[i][j] = a[i] === b[j] ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
    }
  }

  const result: DiffLine[] = [];
  let i = 0;
  let j = 0;
  while (i < a.length && j < b.length) {
    if (a[i] === b[j]) {
      result.push({ kind: "same", text: b[j], newLine: j + 1 });
      i++;
      j++;
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
      result.push({ kind: "removed", text: a[i] });
      i++;
    } else {
      result.push({ kind: "added", text: b[j], newLine: j + 1 });
      j++;
    }
  }
  while (i < a.length) result.push({ kind: "removed", text: a[i++] });
  while (j < b.length) {
    result.push({ kind: "added", text: b[j], newLine: j + 1 });
    j++;
  }
  return result;
}
