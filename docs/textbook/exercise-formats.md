# Notare exercise formats

The interactive blocks a Notare lesson is built from. Writers use this to author exercises. It is also the starting spec for the textbook engine (roadmap stage 3), where each format becomes a block type stored in a lesson's block document. Voice and example rules are in [style-guide.md](style-guide.md).

Every exercise has:

- a permanent `id` that survives edits and new editions, so progress and gradebook entries never orphan;
- `mistakes`: the mistake IDs it practices (shared with lesson metadata);
- `framework`: AP topic codes, when it maps to one.

What Notare can check today is noted for each format. "Needs engine" means stage 3 work.

## 1. Run it (`run`)

A runnable example. The student can edit and run it; nothing is graded.

- **Author provides:** the code, and optionally a one-line prompt ("Change the fare to 3.15 and run again").
- **Rules:** one `public class Main`, no input, output copied from a real run (see the style guide).
- **Today:** the workspace's Run already does this. Needs engine: embedding it in a lesson page.

## 2. Predict, then run (`predict-run`)

The student commits to what a program will print, and only then can they run it. The gap between the guess and the result is the lesson.

- **Author provides:** code; the question ("What does this print?"); an answer type: exact output (free text, compared after trimming trailing whitespace), or multiple choice (3 or 4 outputs, each wrong one built from a specific mistake); and an explanation shown after the run.
- **Checked by:** comparing the guess to the real run's output, not to an answer the author typed, so the key can never drift from the code.
- **Recorded:** the first guess, kept even after retries, so a teacher can see who guessed wrong and how.
- **Needs engine.**

## 3. Review this (`review-this`)

Notare's signature format. The student reviews a short, flawed program the way a teacher reviews a submission in Notare: they click lines and leave comments.

- **Author provides:** the program (10 to 30 lines) with 1 to 3 planted issues. Each issue has the line or lines it lives on, a category (`bug`, `edge-case`, `style`, `naming`), the mistake ID, and a model comment. Optionally, a note that the code is fine and the student should say so (some reviews should find nothing).
- **Checked by:** a planted issue counts as found when the student comments on one of its lines. With Sage on, Sage also judges whether the comment names the actual problem and drafts feedback on it. With Sage off, the model comment is shown after the student submits.
- **Shown after submit:** each planted issue, the student's comment beside it, and the model comment.
- **Today:** line comments exist on submissions (step 2 of the navigation redesign). Needs engine: planted issues and matching.

## 4. Fix it (`fix-it`)

A program that fails: it doesn't compile, or it prints the wrong thing. The student edits it until the tests pass.

- **Author provides:** the broken code, the tests, and a reference fix.
- **Rules:** one bug per exercise, so passing means the student found *the* bug. The compiler error or wrong output is shown up front, exactly as the sandbox prints it.
- **Checked by:** the tests (see "Tests" below).
- **Needs engine** for tests. Run works today.

## 5. Write it (`write-it`)

The student writes code from a description: a method body, a whole class, or a `main` that prints a result.

- **Author provides:** the instructions, starter code (a method signature, or a class with `// TODO` bodies), the tests, and a reference solution.
- **Checked by:** the tests. The reference solution must pass every test in the sandbox, and an exercise whose own solution fails is rejected before anyone sees it (this applies to Sage drafts too).
- **Shown after passing:** the reference solution, labeled as one possible answer.
- **Needs engine** for tests. Coding assignments with Run work today.

## 6. Trace (`trace`)

The student steps through code line by line and sees variables, references and arrays drawn as they change. Best for objects and aliasing, nested loops, and 2D arrays.

- **Author provides:** the code, and optional checkpoints: at line N, the student is asked the value of a variable before the stepper shows it.
- **Checked by:** checkpoint answers compared to the real values at that step.
- **Needs engine** (a stepper; the largest piece of new work in this list).

## 7. Multiple choice (`choice`)

The AP exam's own format, so students get used to it.

- **Author provides:** the question (often with code), 4 options, the correct one, and **an explanation for every wrong option** that names the mistake that leads to it.
- **Rules:** wrong options come from real mistakes, never from filler. No "all of the above" or "none of the above". When options are outputs, each is produced by actually running a mistaken version of the code.
- **Today:** quizzes support multiple choice and true/false. Needs engine: per-option explanations and embedding in lessons.

## 8. Short answer (`short`)

A sentence or two in the student's own words ("Why does this loop never end?").

- **Author provides:** the question, a model answer, and what a good answer must mention.
- **Checked by:** the teacher, with Sage drafting a suggested score and feedback when Sage is on. Same rule as today: nothing Sage drafts reaches the student until the teacher releases it.
- **Today:** quiz short-answer questions work this way.

## Tests

`fix-it` and `write-it` are graded by tests that run in the sandbox. Until the engine defines a test format, write tests as:

- **Output tests:** run `main` and compare printed output (trailing whitespace ignored).
- **Method tests:** a call and its expected result, for example `balanceAfterTaps(10.00, 3) == 2.50` with a 2.50 fare, with doubles compared to within 0.001.

Every exercise needs a test for the normal case, at least one edge case (empty array, zero, the last index), and a test aimed at each mistake it lists. Test failure messages say what was expected in plain words: "With 0 taps the balance should stay 10.0, but it was 7.5".

## Formats we're not using

Drag-to-reorder code puzzles and fill-in-the-blank code are common elsewhere. Notare doesn't use them: review this, predict-run and fix-it practice the same reading skills with real, runnable code.
