#!/usr/bin/env node
// PreToolUse guard for Bash|PowerShell: blocks direct database access outright,
// and forces a confirmation prompt for git push and file deletion, regardless of
// permission mode. Scans the full command text (not just its prefix) so compound
// commands like `docker exec db psql ...` or `build && git push` are still caught.

let input = '';
process.stdin.on('data', (d) => { input += d; });
process.stdin.on('end', () => {
  let payload;
  try {
    payload = JSON.parse(input);
  } catch {
    process.exit(0);
  }

  const rawCmd = (payload.tool_input && payload.tool_input.command) || '';
  if (!rawCmd) process.exit(0);

  // Heredoc bodies (as used for git commit -m "$(cat <<'EOF' ... EOF)") are
  // literal, never-executed text - strip them so prose mentioning DB tools,
  // "git push", or "rm" (like this very hook's own commit message) doesn't
  // trip the scanner. Handles quoted/unquoted/backslash-escaped delimiters.
  const cmd = rawCmd.replace(
    /<<-?\s*(['"]?)([A-Za-z_][A-Za-z0-9_]*)\1[^\n]*\n([\s\S]*?)\n\s*\2\b/g,
    ''
  );
  if (!cmd) process.exit(0);

  const dbPatterns = [
    /\bpsql\b/i,
    /\bmysql\b/i,
    /\bmongosh\b/i,
    /\bmongo\b/i,
    /\bsqlcmd\b/i,
    /\bsqlite3\b/i,
    /\bredis-cli\b/i,
    /\bflyway[:\s]+(migrate|clean|repair|undo|baseline)\b/i,
    /\bliquibase\b.*\b(update|rollback|dropAll|changelogSync)\b/i,
    /\brailway\s+connect\b/i,
  ];

  const pushPatterns = [/\bgit\s+push\b/i];

  const deletePatterns = [
    /(^|[\s;&|])rm\s+/i,
    /\bgit\s+rm\b/i,
    /\bgit\s+clean\b/i,
    /\bfind\b.*-delete\b/i,
    /\brimraf\b/i,
    /\bRemove-Item\b/i,
    /\brmdir\b/i,
    /\bdel\s+/i,
    /\brd\s+\/s\b/i,
  ];

  const matches = (patterns) => patterns.some((p) => p.test(cmd));

  let decision = null;
  let reason = '';

  if (matches(dbPatterns)) {
    decision = 'deny';
    reason = 'Database changes must be performed by a human, not Claude. Ask the user to run this command themselves.';
  } else if (matches(pushPatterns)) {
    decision = 'ask';
    reason = 'Every push to GitHub must be explicitly confirmed by the user.';
  } else if (matches(deletePatterns)) {
    decision = 'ask';
    reason = 'File deletion requires explicit user confirmation.';
  }

  if (decision) {
    process.stdout.write(JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PreToolUse',
        permissionDecision: decision,
        permissionDecisionReason: reason,
      },
    }));
  }

  process.exit(0);
});
