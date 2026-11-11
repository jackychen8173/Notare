package com.notare.coderun.dto;

import java.util.List;

/**
 * What the backend sends to the Next.js sandbox route (app/api/code-run/route.ts). The route
 * trusts all of it: who may run what, and which data files come along, is decided here first.
 *
 * @param stdin  the input for a single run
 * @param inputs null for a single run; otherwise one run per entry (tests), answered with a
 *               SandboxTestResponse. The expected outputs never leave the backend.
 */
public record SandboxRunRequest(String code, String stdin, List<File> files, List<String> inputs) {

    public static SandboxRunRequest single(String code, String stdin, List<File> files) {
        return new SandboxRunRequest(code, stdin, files, null);
    }

    public record File(String name, String content) {
    }
}
