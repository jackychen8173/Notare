/**
 * One CSV field. Quotes anything with a comma, quote or line break, and puts a ' in front of text
 * starting with = + - @ (or a tab or carriage return) so a spreadsheet doesn't run it as a formula:
 * a student named "=HYPERLINK(...)" stays text. Numbers are left alone, negative ones included.
 */
export function csvField(value: string | number | null | undefined): string {
  if (value == null) {
    return "";
  }
  if (typeof value === "number") {
    return String(value);
  }
  const safe = /^[=+\-@\t\r]/.test(value) ? `'${value}` : value;
  return /[",\r\n]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe;
}

export function toCsv(rows: (string | number | null | undefined)[][]): string {
  return rows.map((row) => row.map(csvField).join(",")).join("\r\n");
}

/** Saves text as a file in the browser. The BOM makes Excel read it as UTF-8 (accents in names). */
export function downloadCsv(filename: string, csv: string): void {
  const blob = new Blob(["﻿", csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
