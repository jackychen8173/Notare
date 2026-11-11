package com.notare.coderun.dto;

import com.notare.coderun.AssignmentTest;

import java.util.UUID;

/**
 * A test. For students, a hidden test is only its name and points: stdin and expectedOutput are
 * null, so the answer to a hidden test never reaches the browser.
 */
public record TestResponse(UUID id, String name, String stdin, String expectedOutput, int points, boolean hidden) {

    public static TestResponse forTutor(AssignmentTest test) {
        return new TestResponse(test.getId(), test.getName(), test.getStdin(), test.getExpectedOutput(),
                test.getPoints(), test.isHidden());
    }

    public static TestResponse forStudent(AssignmentTest test) {
        if (test.isHidden()) {
            return new TestResponse(test.getId(), test.getName(), null, null, test.getPoints(), true);
        }
        return forTutor(test);
    }
}
