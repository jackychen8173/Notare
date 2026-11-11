import { NextRequest, NextResponse } from "next/server";
import { Sandbox } from "@vercel/sandbox";

import { prepareJavaProgram } from "@/lib/javaProgram";

// The only backend logic that lives in the Next.js app (see CLAUDE.md's coding-environment
// feature section for why) - a thin, auth-free sandbox runner. The Spring Boot backend is the
// sole owner of student/enrollment authorization and decides which data files come along; this
// route trusts CODE_RUN_INTERNAL_SECRET as its only gate and does nothing else security-sensitive.
export const runtime = "nodejs";
export const maxDuration = 60;

const MAX_CODE_LENGTH = 20_000;
const MAX_STDIN_LENGTH = 65_536;
const MAX_FILES = 10;
const MAX_FILE_LENGTH = 200_000;
// Plain file names only, no folders, and nothing that could clash with the program's own files.
const DATA_FILE_NAME = /^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$/;
const MAX_INPUTS = 20;
const SANDBOX_TIMEOUT_MS = 30_000;
// Test mode (several inputs) gets most of the function's 60s, and stops starting new runs once
// the budget is spent so one infinite loop can't use up every test's time.
const TEST_SANDBOX_TIMEOUT_MS = 55_000;
const TEST_BUDGET_MS = 40_000;
const TEST_RUN_TIMEOUT_S = 4;
// Per step, inside the sandbox's own 30s session. A program that runs longer is stopped and
// reported as timed out, with whatever it printed so far.
const COMPILE_TIMEOUT_S = 20;
const RUN_TIMEOUT_S = 10;
const TIMEOUT_EXIT_CODE = 124; // what coreutils `timeout` exits with when it stops the command

interface CodeRunResult {
  compileError: string | null;
  stdout: string;
  stderr: string;
  exitCode: number | null;
  timedOut: boolean;
}

interface RunOutput {
  stdout: string;
  stderr: string;
  exitCode: number | null;
  timedOut: boolean;
}

interface DataFile {
  name: string;
  content: string;
}

function errorResponse(message: string, status: number) {
  return NextResponse.json({ error: message }, { status });
}

/** A problem with the program's shape, shown to the student the way a compile error is. */
function programError(message: string) {
  const result: CodeRunResult = { compileError: message, stdout: "", stderr: "", exitCode: null, timedOut: false };
  return NextResponse.json(result);
}

function parseFiles(value: unknown): DataFile[] | string {
  if (value === undefined || value === null) return [];
  if (!Array.isArray(value) || value.length > MAX_FILES) return `files must be a list of at most ${MAX_FILES}`;
  const files: DataFile[] = [];
  for (const entry of value) {
    const name = (entry as DataFile | null)?.name;
    const content = (entry as DataFile | null)?.content;
    if (typeof name !== "string" || !DATA_FILE_NAME.test(name) || /\.(java|class)$/i.test(name)) {
      return "each file needs a plain name that isn't a .java or .class file";
    }
    if (typeof content !== "string" || content.length > MAX_FILE_LENGTH) {
      return `each file needs text content of at most ${MAX_FILE_LENGTH} characters`;
    }
    files.push({ name, content });
  }
  return files;
}

export async function POST(request: NextRequest) {
  const secret = request.headers.get("x-internal-secret");
  if (!secret || secret !== process.env.CODE_RUN_INTERNAL_SECRET) {
    return errorResponse("Unauthorized", 401);
  }

  let body: { code?: unknown; stdin?: unknown; files?: unknown; inputs?: unknown };
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
  const stdin = body.stdin ?? "";
  if (typeof stdin !== "string" || stdin.length > MAX_STDIN_LENGTH) {
    return errorResponse(`stdin must be text of at most ${MAX_STDIN_LENGTH} characters`, 400);
  }
  const files = parseFiles(body.files);
  if (typeof files === "string") {
    return errorResponse(files, 400);
  }
  // Test mode: one run per input, answered as { compileError, runs }. The expected outputs stay
  // in the Spring backend, which does the comparing.
  const inputs = body.inputs ?? null;
  if (
    inputs !== null &&
    (!Array.isArray(inputs) ||
      inputs.length > MAX_INPUTS ||
      inputs.some((input) => typeof input !== "string" || input.length > MAX_STDIN_LENGTH))
  ) {
    return errorResponse(`inputs must be a list of at most ${MAX_INPUTS} texts`, 400);
  }

  const prepared = prepareJavaProgram(code);
  if (!prepared.ok) {
    return programError(prepared.error);
  }
  const { mainClass, fileName, source } = prepared.program;

  const snapshotId = process.env.JAVA_SANDBOX_SNAPSHOT_ID;
  if (!snapshotId) {
    return errorResponse("Code execution isn't configured yet (missing JAVA_SANDBOX_SNAPSHOT_ID)", 500);
  }

  const sandbox = await Sandbox.create({
    source: { type: "snapshot", snapshotId },
    persistent: false,
    networkPolicy: "deny-all",
    resources: { vcpus: 1 },
    timeout: inputs ? TEST_SANDBOX_TIMEOUT_MS : SANDBOX_TIMEOUT_MS,
  });

  // The JDK snapshot is built on the minimal `vercel/sandbox/ubuntu` image (see
  // scripts/setup-java-sandbox.mjs), which - unlike the default `universal`/`node`/`python`
  // images - doesn't pre-create /vercel/sandbox as a real directory. Create our own working
  // directory explicitly (mkdir -p is idempotent) rather than relying on that path existing.
  const workDir = "/home/ubuntu/run";
  const stdinPath = `${workDir}/.stdin`;

  try {
    await sandbox.runCommand({ cmd: "mkdir", args: ["-p", workDir] });
    await sandbox.writeFiles([
      { path: `${workDir}/${fileName}`, content: Buffer.from(source, "utf8") },
      // Always written, so a program that reads input with nothing typed gets an empty stream
      // (Scanner then says "No line found") instead of waiting forever.
      { path: stdinPath, content: Buffer.from(stdin, "utf8") },
      ...files.map((file) => ({ path: `${workDir}/${file.name}`, content: Buffer.from(file.content, "utf8") })),
      ...(inputs ?? []).map((input, index) => ({ path: `${workDir}/.in${index}`, content: Buffer.from(input, "utf8") })),
    ]);

    const compile = await sandbox.runCommand({
      cmd: "timeout",
      args: [String(COMPILE_TIMEOUT_S), "javac", fileName],
      cwd: workDir,
    });

    if (compile.exitCode !== 0) {
      const compileError = compile.exitCode === TIMEOUT_EXIT_CODE ? "Compiling took too long." : await compile.stderr();
      if (inputs) {
        return NextResponse.json({ compileError, runs: [] });
      }
      const result: CodeRunResult = {
        compileError,
        stdout: "",
        stderr: "",
        exitCode: compile.exitCode,
        timedOut: false,
      };
      return NextResponse.json(result);
    }

    // mainClass is a Java identifier (checked by the declaration pattern), so it's safe in sh.
    const runWith = async (inputPath: string, seconds: number): Promise<RunOutput> => {
      const run = await sandbox.runCommand({
        cmd: "sh",
        args: ["-c", `timeout ${seconds} java -Xmx256m ${mainClass} < ${inputPath}`],
        cwd: workDir,
      });
      return {
        stdout: await run.stdout(),
        stderr: await run.stderr(),
        exitCode: run.exitCode,
        timedOut: run.exitCode === TIMEOUT_EXIT_CODE,
      };
    };

    if (inputs) {
      const started = Date.now();
      const runs: RunOutput[] = [];
      for (let index = 0; index < inputs.length; index++) {
        if (Date.now() - started > TEST_BUDGET_MS) {
          runs.push({ stdout: "", stderr: "Not run: the tests ran out of time.", exitCode: null, timedOut: true });
          continue;
        }
        runs.push(await runWith(`${workDir}/.in${index}`, TEST_RUN_TIMEOUT_S));
      }
      return NextResponse.json({ compileError: null, runs });
    }

    const result: CodeRunResult = { compileError: null, ...(await runWith(stdinPath, RUN_TIMEOUT_S)) };
    return NextResponse.json(result);
  } catch (error) {
    // runCommand only rejects on an actual failure to run - in practice here that means the
    // sandbox's own session timeout elapsed mid-command.
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
