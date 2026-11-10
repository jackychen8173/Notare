# Notare textbook style guide

How Notare's own books are written, starting with AP Computer Science A. It applies to lessons a person writes and to lessons Sage drafts; Sage's authoring prompts should quote it. Teachers writing their own books can ignore it.

Exercise formats have their own spec: [exercise-formats.md](exercise-formats.md).

## What makes a Notare lesson

Notare is organized around reviewing code, and the book is too. Four rules:

1. **Code comes first.** A lesson opens with a short program that runs, not with a definition. The explanation is about code the student has already seen work.
2. **Every idea gets a bug.** Each lesson names the mistake people actually make with its idea and shows it failing, with the real compiler error or the real wrong output.
3. **Students review before they write.** Before writing code from scratch, the student reviews a flawed program (the "review this" format), the same way a teacher reviews theirs in Notare.
4. **Nothing is read-only for long.** At most about 250 words of prose come between two things the student does: run, predict, review, answer.

## Originality

- Everything is written for Notare. Don't copy, paraphrase or rearrange text, examples or exercises from another textbook, course or website, including free ones.
- Facts are fine, wording isn't. "A `for` loop has an initializer, a condition and an update" is a fact. A sentence or example lifted from another book is not, even reworded.
- The AP course framework says *what* to cover. Cite its topic codes in lesson metadata (see below) but don't quote its text in lessons.
- Sources a writer looked at go in the lesson's private notes, not the page. Linking out to an official doc (for example the Java API reference) is fine.
- Sage drafts follow the same rule, and a person reviews every draft before it's published.

## Voice

- Talk to one student, in the second person: "you", not "we" or "the reader".
- Short sentences, plain words. Define a term the first time it appears, in the same sentence.
- Be direct about what's hard. "This trips almost everyone up the first time" is fine. "Simply" and "just" are not: if it were simple, it wouldn't need a lesson.
- No jokes that need a cultural reference to land, no exclamation marks, no emoji.
- Spelling: US spelling, matching Java's own names (`Color`, `initialize`) and the AP exam.
- Name things the way Java does. Say "method", not "function"; "instance variable", not "field" (unless contrasting them); `ArrayList`, not "list" when the type matters.

## Example programs

The examples use a small set of recurring, everyday settings, so students meet familiar objects again in new units. They're ours, a little West Coast, and they don't depend on any one country's school culture:

| Setting | Good for |
|---|---|
| A transit fare card (balance, taps, top-ups, transfers) | variables, arithmetic, `if`, methods, classes |
| A bike-share dock (bikes in, bikes out, capacity) | objects and state, boolean logic, invariants |
| A weather station (hourly readings) | loops, arrays, averages, min/max, `ArrayList` |
| A music playlist (songs, durations, shuffle) | `String` methods, `ArrayList`, searching, sorting |
| A trail map (a grid of elevations) | 2D arrays, neighbors, paths |
| A small café (orders, menu items, totals) | classes with several fields, `double` vs `int` money pitfalls |

Rules for example code:

- Runnable examples are one file with `public class Main` and a `main` method, because the sandbox compiles one public class per file named after it.
- **No keyboard input.** The sandbox has no stdin, so no `Scanner(System.in)`. Put input data in variables or arrays at the top of `main`.
- No graphics, threads, files or network.
- Stay inside the Java the AP exam uses. Anything outside it (for example `var`, streams, lambdas, `switch` expressions) appears only in a clearly marked "Beyond the exam" callout.
- Java style: 4-space indent, braces on the same line, `camelCase` names that say what the value is (`balance`, not `b`), one statement per line.
- Keep examples under about 25 lines. A longer program goes in a lesson file tab, with the part that matters shown in the lesson.
- Every example is run before it's published, and its output in the lesson is copied from that run, never typed by hand.

## Lesson structure

Lessons open like files in Notare's editor: the explanation on one side, the lesson's example files in tabs on the other. A lesson has these parts, in this order:

1. **Title and goal.** The title names the idea (`Counting with for loops`). The goal is one sentence starting "You'll be able to ...".
2. **First program.** A runnable example, with a one-line prompt ("Run it, then change `5` to `50`").
3. **What's happening.** The explanation, tied to specific lines of the first program by line number.
4. **Predict, then run.** At least one checkpoint where the student commits to an output before running.
5. **The bug.** The common mistake with this idea, shown failing, with the real error or output, and how to read it.
6. **Review this.** A flawed program to review with line comments.
7. **Write it.** One or more coding exercises with tests.
8. **Check.** Two to four multiple-choice or short-answer questions in the exam's style.
9. **Recap.** Three bullets at most: what the student can do now. No new information.

A lesson should take a student 30 to 45 minutes. Split one that runs longer.

### Lesson metadata

Each lesson carries, outside the page itself:

- `framework`: the AP CSA topic codes it covers (for example `["1.4", "1.5"]`), checked against the College Board's current course framework when the lesson is written;
- `prerequisites`: the lessons it assumes;
- `mistakes`: short IDs for the mistakes it teaches (for example `off-by-one-loop`, `int-division`), so assignments can bring old ones back later;
- `status`: `draft`, `in-review` or `published`, plus who wrote and who reviewed it (Sage or a person).

## Formatting

- Headings are sentence case. The title is the only H1; sections are H2.
- Code in a sentence goes in backticks: `count`, `length()`, `ArrayList<String>`.
- Callouts, used sparingly, of three kinds only: **Exam tip** (how the exam tests this), **Watch out** (a trap), **Beyond the exam** (extra Java, safe to skip).
- Error messages and program output are shown exactly as the sandbox prints them, in a monospace block.
- No screenshots of code. Code is always text, so it can be copied and run.

## Sage drafting rules

When Sage drafts a lesson or exercise:

- it follows this guide and the exercise formats spec;
- every program it writes is run in the sandbox, and a draft whose code doesn't compile, or whose stated output doesn't match the run, is rejected before a person sees it;
- it marks anything it isn't sure of (a framework code, an exam claim) for the reviewer instead of stating it;
- it never reproduces text from other books, even when asked.
