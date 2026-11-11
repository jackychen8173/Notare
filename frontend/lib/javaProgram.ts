// Turns the text in a student's editor into one compilable Java file, for the sandbox route
// (app/api/code-run/route.ts). Students can write several classes in one editor, including more
// than one `public class` (Dog and Main, the way AP CSA lessons show them). Java allows only one
// public top-level type per file, so the class with `main` keeps `public` and the others have the
// word blanked out with spaces: same line and column numbers in compiler errors, and in a single
// default-package program a package-private class behaves the same.
//
// Not a parser: comments, strings, char literals and text blocks are masked out first, then
// declarations are found at brace depth 0 and `main` at depth 1.

export type PreparedProgram = {
  mainClass: string;
  fileName: string;
  source: string;
};

export type PrepareResult = { ok: true; program: PreparedProgram } | { ok: false; error: string };

/** The source with comments and literals replaced by spaces (newlines kept), same length. */
export function maskJava(code: string): string {
  const out = code.split("");
  let i = 0;
  const blank = (from: number, to: number) => {
    for (let k = from; k < to && k < out.length; k++) {
      if (out[k] !== "\n") out[k] = " ";
    }
  };
  while (i < code.length) {
    const c = code[i];
    const next = code[i + 1];
    if (c === "/" && next === "/") {
      const end = code.indexOf("\n", i);
      const stop = end === -1 ? code.length : end;
      blank(i, stop);
      i = stop;
    } else if (c === "/" && next === "*") {
      const end = code.indexOf("*/", i + 2);
      const stop = end === -1 ? code.length : end + 2;
      blank(i, stop);
      i = stop;
    } else if (code.startsWith('"""', i)) {
      const end = code.indexOf('"""', i + 3);
      const stop = end === -1 ? code.length : end + 3;
      blank(i, stop);
      i = stop;
    } else if (c === '"' || c === "'") {
      let k = i + 1;
      while (k < code.length && code[k] !== c && code[k] !== "\n") {
        k += code[k] === "\\" ? 2 : 1;
      }
      const stop = Math.min(k + 1, code.length);
      blank(i, stop);
      i = stop;
    } else {
      i++;
    }
  }
  return out.join("");
}

const TYPE_DECLARATION =
  /\b((?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*)(class|interface|enum|record)\s+([A-Za-z_$][A-Za-z0-9_$]*)/g;
const MAIN_METHOD = /\b(?:(?:public|static|final)\s+)+void\s+main\s*\(/g;

type TypeDeclaration = { index: number; name: string; publicAt: number | null };

export function prepareJavaProgram(code: string): PrepareResult {
  const masked = maskJava(code);

  const depthAt: number[] = new Array(masked.length + 1);
  let depth = 0;
  for (let k = 0; k < masked.length; k++) {
    depthAt[k] = depth;
    if (masked[k] === "{") depth++;
    else if (masked[k] === "}") depth = Math.max(0, depth - 1);
  }
  depthAt[masked.length] = depth;

  const types: TypeDeclaration[] = [];
  for (const match of masked.matchAll(TYPE_DECLARATION)) {
    const index = match.index ?? 0;
    if (depthAt[index] !== 0) continue;
    const modifiers = match[1] ?? "";
    const publicOffset = modifiers.search(/\bpublic\b/);
    types.push({
      index,
      name: match[3],
      publicAt: publicOffset === -1 ? null : index + publicOffset,
    });
  }

  if (types.length === 0) {
    return { ok: false, error: "No class found. Your program needs a class with a main method." };
  }

  const withMain = new Set<string>();
  for (const match of masked.matchAll(MAIN_METHOD)) {
    const index = match.index ?? 0;
    if (depthAt[index] !== 1 || !/\bstatic\b/.test(match[0])) continue;
    const owner = [...types].reverse().find((type) => type.index < index);
    if (owner) withMain.add(owner.name);
  }

  const candidates = types.filter((type) => withMain.has(type.name));
  if (candidates.length === 0) {
    return {
      ok: false,
      error: "No main method found. Add public static void main(String[] args) to the class that starts your program.",
    };
  }
  // Several classes with main (a tester plus leftovers): run the public one, else the first.
  const main = candidates.find((type) => type.publicAt !== null) ?? candidates[0];

  const chars = code.split("");
  for (const type of types) {
    if (type.name === main.name || type.publicAt === null) continue;
    for (let k = type.publicAt; k < type.publicAt + "public".length; k++) chars[k] = " ";
  }

  return {
    ok: true,
    program: { mainClass: main.name, fileName: `${main.name}.java`, source: chars.join("") },
  };
}
