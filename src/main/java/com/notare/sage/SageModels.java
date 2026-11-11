package com.notare.sage;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.StopReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Which Claude model each Sage job runs on, and the request settings the jobs share. Model ids are
 * plain strings: the pinned anthropic-java (2.34.0) predates the Model constants for these models.
 *
 * <p>Both models think by default (adaptive thinking), so responses can start with thinking blocks
 * and thinking counts toward max_tokens: read blocks by type and leave room in max_tokens. Lower
 * the effort level to get less thinking; there's no "thinking off" on these models worth using.
 */
public final class SageModels {

    private static final Logger log = LoggerFactory.getLogger(SageModels.class);

    /** Code review, quiz grading and the tutor chat: judgment calls and multistep tool use. */
    public static final String SONNET = "claude-sonnet-5-5";

    /** Short rewriting and summarizing (session notes, progress summaries): cheap and fast. */
    public static final String HAIKU = "claude-haiku-5-5";

    /**
     * Server-side refusal fallback (Sonnet only; Haiku 5.5 has none). With fallbacks "default",
     * a classifier refusal in a category it covers is retried on another model instead of failing.
     */
    public static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    /** Lets the chat ask for thinking blocks to be dropped, not rejected, if history ever differs. */
    public static final String THINKING_BINDING_BETA = "thinking-binding-controls-2026-08-01";

    public static final JsonValue FALLBACKS_DEFAULT = JsonValue.from("default");

    private SageModels() {
    }

    /**
     * Stops a refused or cut-off answer from being treated as a real one. A refusal is a normal
     * 200 from the API, so it has to be checked before reading the content.
     */
    public static void requireAnswer(Optional<StopReason> stopReason, Map<String, JsonValue> additionalProperties) {
        if (isRefusal(stopReason)) {
            // WARN, not ERROR: a refusal isn't a bug, and ERROR goes to Sentry. stop_details holds
            // only the refusal category and a generic explanation, never the request's content.
            log.warn("Sage request was declined by the model: {}", additionalProperties.get("stop_details"));
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Sage declined this request. Try rephrasing it, or write this one yourself.");
        }
        if (stopReason.filter(StopReason.MAX_TOKENS::equals).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Sage's answer was cut off. Please try again.");
        }
    }

    public static boolean isRefusal(Optional<StopReason> stopReason) {
        return stopReason.filter(StopReason.REFUSAL::equals).isPresent();
    }

    /**
     * The chat history is append-only, so the API should never have to drop a thinking block
     * because the history changed. If it does, say so in the log: it means something now edits
     * stored messages, which costs Sage its earlier reasoning and the prompt cache.
     */
    public static void logDroppedThinking(Message response) {
        JsonValue transformations = response._additionalProperties().get("input_transformations");
        if (transformations == null) {
            return;
        }
        List<?> entries = transformations.convert(List.class);
        if (entries == null) {
            return;
        }
        long historyMismatches = entries.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(entry -> "prefix_binding_mismatch".equals(entry.get("reason")))
                .count();
        if (historyMismatches > 0) {
            log.warn("Sage chat history no longer matched for {} thinking block(s); they were dropped",
                    historyMismatches);
        }
    }
}
