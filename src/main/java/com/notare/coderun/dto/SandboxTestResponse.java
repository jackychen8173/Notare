package com.notare.coderun.dto;

import java.util.List;

/**
 * The sandbox's answer to a request with inputs: a compile error (and no runs), or one run per
 * input, in the same order. A run that wasn't started because time ran out is timedOut.
 */
public record SandboxTestResponse(String compileError, List<Run> runs) {

    public record Run(String stdout, String stderr, Integer exitCode, boolean timedOut) {
    }
}
