package com.notare.coderun.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The outcome of running an assignment's tests on some code.
 *
 * @param compileError   set when the code didn't compile; then every result failed
 * @param suggestedScore the assignment's points scaled by the share of test points earned, or
 *                       null when the assignment isn't scored in points. Only a suggestion: the
 *                       tutor decides the score.
 * @param ranAt          when the run happened (stored runs on a submission); null for a check
 */
public record TestRunResponse(
        String compileError,
        List<Result> results,
        int pointsEarned,
        int pointsPossible,
        BigDecimal suggestedScore,
        LocalDateTime ranAt
) {

    /**
     * One test's outcome. For students, a hidden test's input, expected output, actual output and
     * error text are null: only its name, points and whether it passed.
     */
    public record Result(
            UUID testId,
            String name,
            int points,
            boolean hidden,
            boolean passed,
            boolean timedOut,
            String stdin,
            String expectedOutput,
            String actualOutput,
            String stderr
    ) {
        public Result forStudent() {
            if (!hidden) {
                return this;
            }
            return new Result(testId, name, points, true, passed, timedOut, null, null, null, null);
        }
    }

    public TestRunResponse forStudent() {
        return new TestRunResponse(compileError, results.stream().map(Result::forStudent).toList(),
                pointsEarned, pointsPossible, null, ranAt);
    }
}
