package com.notare.coderun.dto;

public record CodeRunResponse(
        // Set (only) when javac fails - stdout/stderr/exitCode/timedOut are meaningless in that
        // case, since the java step never ran.
        String compileError,
        String stdout,
        String stderr,
        Integer exitCode,
        boolean timedOut
) {
}
