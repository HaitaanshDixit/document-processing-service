# AI Usage

## Tools used
- Claude (Anthropic) — used to scaffold and write the backend (Spring Boot service, entities, Redis-backed
  queue/lock/duplicate-detection, retry orchestration, controller, tests).

## What it was used for
- Generating the initial project structure and all Java source files based on the assignment spec.
- Proposing the Redis usage pattern (queue + SETNX duplicate detection + Lua-script-guarded lock).
- Drafting the retry state machine using Spring Retry's `RetryTemplate`.
- Writing the test suite skeleton for the required scenarios.

## What I changed / reviewed
- (Fill in as you review: e.g. adjusted retry/backoff numbers, tightened validation rules, renamed fields to
  match the exact JSON shape in the assignment PDF, verified the Redis lock's compare-and-delete script is
  actually atomic.)

## What I verified independently
- (Fill in: e.g. ran `docker compose up` and confirmed Postgres/Redis start; ran the test suite; manually
  uploaded a file twice and confirmed the duplicate response; killed the app mid-processing and confirmed the
  stale-document recovery scheduler requeues it.)

## Anything learned from reviewing AI output
- (Fill in as you go — e.g. anything you had to correct, any assumption the AI made that didn't match the
  assignment.)
