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

/** A teacher-written output check. For students, a hidden test has no stdin or expectedOutput. */
export interface AssignmentTest {
  id: string;
  name: string;
  stdin: string | null;
  expectedOutput: string | null;
  points: number;
  hidden: boolean;
}

export interface TestResult {
  testId: string;
  name: string;
  points: number;
  hidden: boolean;
  passed: boolean;
  timedOut: boolean;
  // Null on a student's view of a hidden test.
  stdin: string | null;
  expectedOutput: string | null;
  actualOutput: string | null;
  stderr: string | null;
}

export interface TestRunResult {
  compileError: string | null;
  results: TestResult[];
  pointsEarned: number;
  pointsPossible: number;
  /** The assignment's points scaled by the test points earned; teachers only. */
  suggestedScore: number | null;
  ranAt: string | null;
}
