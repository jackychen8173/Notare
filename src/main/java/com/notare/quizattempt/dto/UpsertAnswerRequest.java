package com.notare.quizattempt.dto;

import java.util.UUID;

// One or the other is set, depending on the question's type: selectedOptionId for MULTIPLE_CHOICE/
// TRUE_FALSE, textResponse for SHORT_ANSWER/ESSAY. Not cross-validated against the question here -
// an option ID for a text question or vice versa is simply ignored at grading time.
public record UpsertAnswerRequest(
        UUID selectedOptionId,
        String textResponse
) {
}
