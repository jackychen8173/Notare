package com.notare.sage;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Sage's structured review of a code submission: the four overall sections (stored as
 * {@link SageFeedback}) plus comments pinned to specific lines (stored as SUGGESTED line comments
 * the tutor accepts or dismisses).
 */
public record SageCodeReview(
        String correctness,
        String style,
        String suggestions,
        String encouragement,
        @JsonPropertyDescription("Comments on specific lines of the submission, using the line numbers shown in the code")
        List<LineNote> lineComments
) {
    public record LineNote(
            @JsonPropertyDescription("1-based line number, as shown in the numbered code")
            int line,
            @JsonPropertyDescription("One or two sentences about this specific line, written to the student")
            String comment
    ) {
    }

    public SageFeedback overall() {
        return new SageFeedback(correctness, style, suggestions, encouragement);
    }
}
