import { NextRequest, NextResponse } from "next/server";
import { Sandbox } from "@vercel/sandbox";

// The only backend logic that lives in the Next.js app (see CLAUDE.md's coding-environment
// feature section for why) - a thin, auth-free sandbox runner. The Spring Boot backend is the
// sole owner of student/enrollment authorization; this route trusts CODE_RUN_INTERNAL_SECRET as
// its only gate and does nothing else security-sensitive on its own.
export const runtime = "nodejs";
export const maxDuration = 60;

const MAX_CODE_LENGTH = 20_000;
const SANDBOX_TIMEOUT_MS = 30_000;

// Heuristic, not a real parser - matches CSAwesome's own "filename matches the class name" rule.
// A `public class` mentioned inside a comment or string would false-positive; acceptable for v1,
// same order of sophistication as everything else in this endpoint.
const PUBLIC_CLASS_PATTERN = /public\s+(?:final\s+|abstract\s+)?class\s+([A-Za-z_$][A-Za-z0-9_$]*)/g;

interface CodeRunResult {
  compileError: string | null;
  stdout: string;
  stderr: string;
  exitCode: number | null;
  timedOut: boolean;
}

function errorResponse(message: string, status: number) {
  return NextResponse.json({ error: message }, { status });
}

export async function POST(request: NextRequest) {
  const secret = request.headers.get("x-internal-secret");
  if (!secret || secret !== process.env.CODE_RUN_INTERNAL_SECRET) {
    return errorResponse("Unauthorized", 401);
  }

  let body: { code?: unknown };
  try {
    body = await request.json();
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const code = body.code;
  if (typeof code !== "string" || !code.trim()) {
    return errorResponse("code is required", 400);
  }
  if (code.length > MAX_CODE_LENGTH) {
    return errorResponse(`code exceeds the ${MAX_CODE_LENGTH}-character limit`, 400);
  }

  const classMatches = [...code.matchAll(PUBLIC_CLASS_PATTERN)];
  if (classMatches.length === 0) {
    return errorResponse("No public class found — your program needs exactly one `public class`.", 400);
  }
  if (classMatches.length > 1) {
    return errorResponse(
      "Multiple public classes found — your program can only have one `public class`.",
      400,
    );
  }
  const className = classMatches[0][1];

  const snapshotId = process.env.JAVA_SANDBOX_SNAPSHOT_ID;
  if (!snapshotId) {
    return errorResponse("Code execution isn't configured yet (missing JAVA_SANDBOX_SNAPSHOT_ID)", 500);
  }

  const sandbox = await Sandbox.create({
    source: { type: "snapshot", snapshotId },
    persistent: false,
    networkPolicy: "deny-all",
    resources: { vcpus: 1 },
    timeout: SANDBOX_TIMEOUT_MS,
  });

  // The JDK snapshot is built on the minimal `vercel/sandbox/ubuntu` image (see
  // scripts/setup-java-sandbox.mjs), which - unlike the default `universal`/`node`/`python`
  // images - doesn't pre-create /vercel/sandbox as a real directory. Create our own working
  // directory explicitly (mkdir -p is idempotent) rather than relying on that path existing.
  const workDir = "/home/ubuntu/run";

  try {
    await sandbox.runCommand({ cmd: "mkdir", args: ["-p", workDir] });
    await sandbox.writeFiles([
      { path: `${workDir}/${className}.java`, content: Buffer.from(code, "utf8") },
    ]);

    const compile = await sandbox.runCommand({
      cmd: "javac",
      args: [`${className}.java`],
      cwd: workDir,
    });

    if (compile.exitCode !== 0) {
      const result: CodeRunResult = {
        compileError: await compile.stderr(),
        stdout: "",
        stderr: "",
        exitCode: compile.exitCode,
        timedOut: false,
      };
      return NextResponse.json(result);
    }

    const run = await sandbox.runCommand({
      cmd: "java",
      args: [className],
      cwd: workDir,
    });

    const result: CodeRunResult = {
      compileError: null,
      stdout: await run.stdout(),
      stderr: await run.stderr(),
      exitCode: run.exitCode,
      timedOut: false,
    };
    return NextResponse.json(result);
  } catch (error) {
    // runCommand only rejects on an actual failure to run - in practice here that means the
    // sandbox's own session timeout elapsed mid-command (an infinite loop, etc.).
    const result: CodeRunResult = {
      compileError: null,
      stdout: "",
      stderr: error instanceof Error ? error.message : "Execution failed",
      exitCode: null,
      timedOut: true,
    };
    return NextResponse.json(result);
  } finally {
    await sandbox.stop();
  }
}
