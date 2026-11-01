# OpenSpec Workflow: Hotel Reservation System (PoC)

Plan for rebuilding the Hotel Reservation System with [OpenSpec](https://github.com/Fission-AI/OpenSpec)
(CLI `@fission-ai/openspec`, Claude Code slash commands `/opsx:*`), so it can be compared with:

- **AIUP** on branch `hotel-reservation` (complete: 25 use cases, 2 change requests, see `docs/aiup-workflow.md` there)
- **specs.md** on branch `hotel-reservation-specsmd` (inception done, 4 bolts built)
- **cc-sdd** on branch `hotel-reservation-cc-sdd` (stopped during `/kiro-spec-batch`: token limit used up)

Flow: **install → project context → explore → per change: proposal → specs → design → tasks → apply → verify →
archive → change requests → comparison**

## Quick run sheet (from zero)

Short list of every command in order; details for each step are further down.

```bash
# 0. Install (docs/vision.md is already on this branch)
npm install -g @fission-ai/openspec@latest   # installs the openspec CLI on your machine
openspec --version                       # optional: note the version for the comparison
openspec init --tools claude             # creates openspec/ and the /opsx:* commands for Claude Code
openspec config profile                  # choose the expanded workflow (adds /opsx:new, /opsx:continue …)
openspec update                          # rewrites the /opsx:* files in this repo for that profile
#    → commit "Install OpenSpec <version>"

# 1. Project context
#    edit openspec/config.yaml (context + rules, see step 1)
#    → commit "Add OpenSpec project context"
```

```
# 2. Plan the changes
/opsx:explore "Build the Hotel Reservation System described in docs/vision.md. Which changes, in which order?"
```

Example for the first change, `add-staff-access`:

```
# 3. Plan
/opsx:new add-staff-access
/opsx:continue add-staff-access              # repeat until all documents exist (see step 3)
openspec status --change add-staff-access    # shows which documents are done
openspec validate add-staff-access --strict
#    → commit "Plan change add-staff-access"

# 4. Implement
/opsx:apply add-staff-access

# 5. Verify and archive
./mvnw verify
/opsx:verify add-staff-access
/opsx:archive add-staff-access
#    → commit "Implement change add-staff-access with tests"
```

Repeat steps 3–5 the same way for the other changes from step 2 (one session per change, `/clear` in between).

```
# 6. Change requests (same loop as 3–5, with the Request text only)
/opsx:explore "<Request text of CR-001>"   → change-online-cancellation-deadline
/opsx:explore "<Request text of CR-002>"   → add-seasonal-rates

# 7. Run the app and compare
./mvnw spring-boot:run                     → http://localhost:8080
```

## How OpenSpec differs from the other tools

- `openspec/specs/<capability>/spec.md` is the **current truth** of the system, grouped by capability. It starts empty.
- Every piece of work is a **change** in `openspec/changes/<change-name>/` with `proposal.md`, `design.md`,
  `tasks.md` and **delta specs** (`specs/<capability>/spec.md` with `## ADDED / MODIFIED / REMOVED Requirements`).
- `/opsx:archive` merges the delta specs into `openspec/specs/` and moves the change to `openspec/changes/archive/`.
- Requirements are written as `### Requirement:` with `SHALL`/`MUST`, each with `#### Scenario:` blocks
  (WHEN / THEN). There are no use case IDs, entity model or use case diagram.
- **Greenfield works the same way.** In OpenSpec all work is a change, even the very first feature. On a new project
  `openspec/specs/` is empty, so the first changes contain only `## ADDED Requirements`; `/opsx:archive` then writes
  the first specs into `openspec/specs/`. Building the whole system is a series of changes (`add-staff-access`,
  `add-room-catalog`, …). One single change for the whole vision would also work, but it would be far too big to
  review and would hit the token limit like cc-sdd did.
- There is no batch command that creates every spec at once (that is where cc-sdd ran out of tokens). You do one change
  at a time, which also keeps token use under control.

## Progress

| Step | Name                               | Command                                              | Status |
|------|------------------------------------|------------------------------------------------------|--------|
| 0    | Install                            | `openspec init --tools claude`                       | Open   |
| 1    | Project context (= steering)       | edit `openspec/config.yaml`                          | Open   |
| 2    | Explore and plan the changes       | `/opsx:explore`                                      | Open   |
| 3    | Per change: plan                   | `/opsx:new`, `/opsx:continue` ×4                     | Open   |
| 4    | Per change: implement              | `/opsx:apply`                                        | Open   |
| 5    | Per change: verify and archive     | `./mvnw verify`, `/opsx:verify`, `/opsx:archive`     | Open   |
| 6    | Change requests CR-001, CR-002     | `/opsx:explore`, `/opsx:new` … `/opsx:archive`       | Open   |
| 7    | Comparison with AIUP / cc-sdd      | by hand                                              | Open   |

---

## How AIUP steps map to OpenSpec

| AIUP (hotel-reservation branch)      | OpenSpec                                                            | Output                                       |
|--------------------------------------|---------------------------------------------------------------------|----------------------------------------------|
| Install `aiup-vaadin-jooq`           | `openspec init --tools claude`                                      | `openspec/`, `/opsx:*` commands and skills   |
| `docs/vision.md` (by hand)           | Same file, input to `/opsx:explore` and `/opsx:new`                 | –                                            |
| Stack knowledge from the plugin      | `context` and `rules` in `openspec/config.yaml`                     | `openspec/config.yaml`                       |
| `/requirements`, glossary            | Delta specs per change (`### Requirement:` + `#### Scenario:`)      | `changes/<name>/specs/<capability>/spec.md`  |
| `/entity-model`                      | No equivalent; data model in the first change's `design.md`         | `design.md`                                  |
| `/use-case-diagram`                  | No equivalent; `openspec list --specs` lists capabilities           | –                                            |
| `/use-case-spec UC-XXX`              | Requirements + scenarios (alternative flows become scenarios)       | delta specs                                  |
| `/spec-review`                       | `openspec validate --strict` (structure only) + your own review     | –                                            |
| `/flyway-migration`                  | Tasks in each change's `tasks.md`                                   | `tasks.md`                                   |
| `/implement UC-XXX`                  | `/opsx:apply <change>`                                              | code, ticked-off `tasks.md`                  |
| `/browserless-test`, `/playwright-test` | Tasks inside `/opsx:apply`, forced through `rules.tasks` in config | tests                                       |
| `/coverage-check UC-XXX`             | `/opsx:verify <change>`                                             | verification report                          |
| Status lines in the docs             | `openspec status`, `openspec view`, `openspec list`                 | –                                            |
| `docs/change_requests/CR-00x`        | A new change with `## MODIFIED Requirements` deltas                 | change folder → archive                      |

## Step 0 – Install

The branch `hotel-reservation-openspec` starts from `main`; `docs/vision.md` (same input as cc-sdd and specs.md) is
already added.

```bash
npm install -g @fission-ai/openspec@latest           # needs Node >= 22.12 (installed: v24)
openspec --version                                   # optional: write the version into this file
openspec init --tools claude                         # creates openspec/ + the /opsx:* commands for Claude Code
openspec config profile                              # choose the expanded workflow (new, continue, ff, verify …)
openspec update                                      # regenerates the /opsx:* files for the chosen profile
```

- `init` sets up OpenSpec in this repo: the `openspec/` folder (`specs/`, `changes/`, `config.yaml`) and the
  Claude Code slash commands. `--tools claude` skips the tool picker.
- The default profile only has `/opsx:propose`, `/opsx:explore`, `/opsx:apply` and a few more. `config profile`
  switches to the expanded set, and `update` rewrites the slash command files in the repo to match.

Check what `init` created (`openspec/`, `.claude/commands/opsx/` and/or `.claude/skills/openspec-*`) and commit it
as "Install OpenSpec <version>".

**Fairness, same as on the cc-sdd branch:** do not use anything from AIUP. The AIUP plugins are installed globally
and react to words like "write requirements" or "implement"; always call the `/opsx:*` commands by name, or turn the
AIUP plugins off for this branch (`/plugin` → disable `aiup-core`, `aiup-vaadin-jooq`). Do not copy `docs/` from the
AIUP branch except `vision.md` (and the change request texts in step 6).

## Step 1 – Project context (once)

OpenSpec has no steering command. Everything project-wide goes into `openspec/config.yaml`; `context` is sent with
every artifact, `apply` and `archive`, `rules` only with the artifact they name. Keep it short: OpenSpec warns that
long context makes the output worse.

```yaml
schema: spec-driven            # keep what init wrote

context: |
  Hotel Reservation System for one hotel; product vision in docs/vision.md.
  Roles: Guest (no login), Receptionist, Manager, Housekeeper.
  Stack: Java 25, Spring Boot 4, Vaadin 25 Flow (server-side Java views), jOOQ, Flyway, H2, Spring Security.
  Base package ch.martinelli.edu.hrs, one package per feature area, views in <area>/ui.
  Flyway migrations in src/main/resources/db/migration are never edited after commit; add a new one.
  ./mvnw compile runs Flyway against H2 and generates the jOOQ classes; never edit generated code.
  Tests: Vaadin Browserless tests (./mvnw test) for every view, one Playwright test *IT per feature (./mvnw verify).
  Test data in src/test/resources/db/migration, one file per change. Tests use a fixed Clock.

rules:
  specs:
    - Every alternative or error flow from the vision is its own scenario
    - Use the vision's terms for domain concepts; one name per concept
  design:
    - List new tables and columns with types and constraints
  tasks:
    - Every view task has a Browserless test task; every change has one Playwright test task
    - The last task runs ./mvnw verify and must end green
```

Check the field names against the config file `init` wrote and the docs
(<https://openspec.dev/docs/configuration/config-yaml>) before you save it. Commit: "Add OpenSpec project context".

This file is what replaces the AIUP stack plugin; it has the biggest effect on the result.

## Step 2 – Explore and plan the changes

```
/opsx:explore "Build the Hotel Reservation System described in docs/vision.md. Which changes, in which order,
               and which capabilities should the specs be grouped into? Do not write any files."
```

`explore` only thinks and discusses, it writes nothing. Let OpenSpec propose the split first (that is part of what the
PoC compares). If it suggests one huge change, steer it toward a dependency-ordered list like this one:

| # | Change name                        | Covers (AIUP use cases, for the comparison only)                       |
|---|------------------------------------|------------------------------------------------------------------------|
| 1 | `add-staff-access`                 | log in, log out, maintain staff members (UC-021–023)                   |
| 2 | `add-room-catalog`                 | room types, rooms, block room (UC-016–018)                             |
| 3 | `add-online-booking`               | reserve online, view and cancel own reservation (UC-001–003)           |
| 4 | `add-front-desk-reservations`      | create, change, cancel, search reservations (UC-004–007)               |
| 5 | `add-reception`                    | arrivals/departures, check in/out, status board, no-show (UC-008–012)  |
| 6 | `add-housekeeping`                 | cleaning list, mark clean, nightly dirty run (UC-013–015)              |
| 7 | `add-reports`                      | occupancy and revenue reports (UC-019–020)                             |
| 8 | `add-guest-data-erasure`           | erase guest data (UC-024)                                              |

Write the agreed list into this file. Change 1 owns the base schema and login, so it goes first.

## Step 3 – Per change: plan (human review at each artifact)

A change has four documents that build on each other: proposal → specs → design → tasks. `/opsx:new` creates only
the empty folder `openspec/changes/add-staff-access/`. Each `/opsx:continue` then writes **the next missing document
and stops**, so you review and fix it before the next one is written. Four documents, so four calls:

| Call | Command                              | Writes                                       | What to review                                |
|------|--------------------------------------|----------------------------------------------|-----------------------------------------------|
| 1    | `/opsx:new add-staff-access`         | empty change folder                          | the change name                               |
| 2    | `/opsx:continue add-staff-access`    | `proposal.md`                                | why, what, which capabilities are affected    |
| 3    | `/opsx:continue add-staff-access`    | `specs/<capability>/spec.md`                 | requirements + scenarios (review carefully)   |
| 4    | `/opsx:continue add-staff-access`    | `design.md`                                  | tables, packages, views                       |
| 5    | `/opsx:continue add-staff-access`    | `tasks.md`                                   | checklist, including test tasks               |

You don't count the calls yourself. The list of documents comes from the schema in `openspec/config.yaml`
(`spec-driven`: proposal, specs, design, tasks). After each call, `/opsx:continue` reports what it wrote and what is
next; `openspec status --change add-staff-access` shows which documents are done and which are still missing. When
all are done, the change is ready for `/opsx:apply`. An extra `/opsx:continue` does no harm; it just reports that
nothing is left.

`/opsx:ff add-staff-access` writes all four in one go instead: faster, but you only review at the end.

```bash
openspec validate add-staff-access --strict
openspec status --change add-staff-access
```

- Use `/opsx:continue` (one artifact at a time), not `/opsx:ff` or `/opsx:propose` (all at once): the review between
  artifacts is where the PoC is judged, and it lets you count the corrections. If you fix something by hand, run
  `/opsx:update add-staff-access` so the other artifacts stay consistent.
- Check that every alternative flow and business rule from the vision became a `#### Scenario:`.
- Commit the planning artifacts after review: "Plan change add-staff-access".

## Step 4 – Per change: implement

```
/opsx:apply add-staff-access
```

Works through `tasks.md` and ticks the boxes. Unlike cc-sdd there is no separate reviewer subagent per task, so check
the result yourself in step 5.

**Token budget (lesson from cc-sdd):** run one change per session and `/clear` between changes. If the limit runs
out, `/opsx:apply` picks up at the first unticked task.

## Step 5 – Per change: verify and archive

```bash
./mvnw verify
```
```
/opsx:verify add-staff-access        → does the code match proposal, specs, design and tasks?
/opsx:archive add-staff-access       → merges delta specs into openspec/specs/, moves the change to archive/
```

```bash
openspec list --specs                # the capabilities now in the main specs
openspec view                        # dashboard
```

Commit: "Implement change add-staff-access with tests" (code + archive move + updated `openspec/specs/`).

**Repeat steps 3–5 for every change** from step 2, in order. Then run the app with `./mvnw spring-boot:run` and open
<http://localhost:8080>.

## Step 6 – Change requests

Replay the AIUP change requests from the `hotel-reservation` branch, using only the **Request** text (not AIUP's
decisions or use case edits), so OpenSpec has to find the affected requirements itself:

```bash
git show hotel-reservation:docs/change_requests/CR-001-online-cancellation-deadline.md
git show hotel-reservation:docs/change_requests/CR-002-seasonal-rates.md
```

```
/opsx:explore "<Request text of CR-001>"     → which requirements in openspec/specs/ change? open questions?
/opsx:new change-online-cancellation-deadline
/opsx:continue …  (×4)                        → delta specs should be MODIFIED, not ADDED
/opsx:apply change-online-cancellation-deadline
/opsx:verify change-online-cancellation-deadline
/opsx:archive change-online-cancellation-deadline
```

Give OpenSpec the same stakeholder answers AIUP got (the **Decisions** section of the CR) only when it asks the
question; note how many it found by itself. Then the same for CR-002 as `add-seasonal-rates`.

This step is OpenSpec's main selling point (brownfield deltas), so it deserves the closest look.

## Step 7 – Comparison

Same measures as in the cc-sdd plan, so the four tools can go in one table:

- Human corrections at each review step (proposal, specs, design, tasks) compared with AIUP's review edits.
- Coverage: do all 25 AIUP use cases' business rules appear as requirements/scenarios? Check by hand against
  `hotel-reservation:docs/use_cases/` (or run AIUP's `/coverage-check` at the very end, as a neutral check only).
- Number of tests, `./mvnw verify` result, time and token cost per change (and whether it fit into the limit,
  unlike cc-sdd).
- Traceability: AIUP marks code with `UC-XXX BR-YYY`; OpenSpec links code only through `tasks.md`.
- Effort and accuracy for CR-001 and CR-002 (did the MODIFIED deltas hit the right requirements?).
- Quality of `openspec/specs/` at the end as living documentation, compared with AIUP's `docs/`.
