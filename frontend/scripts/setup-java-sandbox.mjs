// One-time (or re-run-when-needed) provisioning script for the coding-environment "Run" feature.
// Creates a Vercel Sandbox, installs a JDK, and snapshots it - every real "Run" request then boots
// from that snapshot instead of installing a JDK per request. Run it locally with a fresh OIDC
// token (`vercel env pull .env.local` from frontend/, then `node --env-file=.env.local
// scripts/setup-java-sandbox.mjs`), and set the printed SNAPSHOT_ID as JAVA_SANDBOX_SNAPSHOT_ID on
// the Vercel project. Re-run this (and update the env var) to bump the JDK version later.
import { Sandbox } from "@vercel/sandbox";

async function main() {
  console.log("Creating base sandbox...");
  const sandbox = await Sandbox.create({
    image: "vercel/sandbox/ubuntu",
    resources: { vcpus: 1 },
    timeout: 300_000,
    persistent: false,
  });

  let snapshotId;
  try {
    console.log("Installing JDK 21...");
    const install = await sandbox.runCommand({
      cmd: "bash",
      args: ["-c", "apt-get update && apt-get install -y openjdk-21-jdk-headless"],
      sudo: true,
    });
    if (install.exitCode !== 0) {
      throw new Error("apt-get install failed: " + (await install.stderr()));
    }

    const check = await sandbox.runCommand("bash", ["-c", "javac -version 2>&1; java -version 2>&1"]);
    console.log("Verify output:\n" + (await check.stdout()));

    console.log("Snapshotting (this stops the sandbox automatically)...");
    const snap = await sandbox.snapshot();
    snapshotId = snap.snapshotId;
  } catch (err) {
    console.error("Setup failed, stopping sandbox:", err);
    await sandbox.stop();
    throw err;
  }

  console.log("SNAPSHOT_ID=" + snapshotId);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
