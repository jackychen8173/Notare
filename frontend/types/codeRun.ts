export interface CodeRunResult {
  // Set (only) when compilation fails - stdout/stderr/exitCode/timedOut are meaningless then,
  // since the program never ran.
  compileError: string | null;
  stdout: string;
  stderr: string;
  exitCode: number | null;
  timedOut: boolean;
}
