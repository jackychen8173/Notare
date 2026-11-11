package com.notare.coderun;

/**
 * Whether a program's output matches a test's expected output. Forgiving only where students
 * can't see the difference: Windows vs Unix line endings, spaces at the end of a line, and blank
 * lines at the end. Everything else (spelling, case, spaces inside a line) has to match.
 */
public final class OutputMatch {

    private OutputMatch() {
    }

    public static boolean matches(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(line.stripTrailing()).append('\n');
        }
        return out.toString().stripTrailing();
    }
}
