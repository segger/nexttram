# AGENTS.md

## General preferences

- Prefer really short answers.
- Do not explain obvious details unless asked.
- Avoid long examples unless they are necessary.
- Just give one option unless asked.

## Local environment

- Sandboxed commands fail in this environment with `bwrap: loopback: Failed RTM_NEWADDR: Operation not permitted`.
- If a command fails due to sandboxing, rerun it with escalation instead of retrying in the sandbox.