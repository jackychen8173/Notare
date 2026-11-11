package com.notare.coderun.dto;

import java.util.List;

/**
 * What the backend sends to the Next.js sandbox route (app/api/code-run/route.ts). The route
 * trusts all of it: who may run what, and which data files come along, is decided here first.
 */
public record SandboxRunRequest(String code, String stdin, List<File> files) {

    public record File(String name, String content) {
    }
}
