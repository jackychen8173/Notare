export interface CodeRunResult {
  // Set (only) when compilation fails - stdout/stderr/exitCode/timedOut are meaningless then,
  // since the program never ran.
  compileError: string | null;
  stdout: string;
  stderr: string;
  exitCode: number | null;
  timedOut: boolean;
}

/** A text file a teacher attaches to an assignment; Run puts it next to the student's program. */
export interface DataFile {
  id: string;
  name: string;
  content: string;
  updatedAt: string;
}
