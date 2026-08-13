package com.notare.sage;

public record SageFeedback(
        String correctness,
        String style,
        String suggestions,
        String encouragement
) {
}
