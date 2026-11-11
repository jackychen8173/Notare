package com.notare.file;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** File-name cleanup and the rules for which files a browser may show inline. */
final class FileNames {

    /** Safe to show in the browser. Anything else (HTML, SVG, scripts) is always a download. */
    private static final Set<String> INLINE_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/gif", "image/webp", "text/plain");

    private FileNames() {
    }

    /** The last path segment, without control characters or quotes, at most 200 characters. */
    static String clean(String original) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            name = "file";
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    /** What the server stores as the type: the browser's claim, normalized, or a generic binary type. */
    static String contentType(String claimed) {
        if (claimed == null || claimed.isBlank() || claimed.length() > 127) {
            return "application/octet-stream";
        }
        return claimed.split(";")[0].trim().toLowerCase();
    }

    static boolean inline(String contentType) {
        return INLINE_TYPES.contains(contentType);
    }

    /** RFC 6266, with an ASCII fallback and the UTF-8 name for browsers that read it. */
    static String contentDisposition(String fileName, boolean inline) {
        String ascii = fileName.replaceAll("[^\\x20-\\x7E]", "_").replace("\\", "_");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return (inline ? "inline" : "attachment") + "; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }
}
