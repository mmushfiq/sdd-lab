# OpenSpec Workflow: Hotel Reservation System

How to build the Hotel Reservation System described in `docs/vision.md` with
[OpenSpec](https://github.com/Fission-AI/OpenSpec) (CLI `@fission-ai/openspec`, Claude Code slash commands `/opsx:*`).
The only inputs are this repository (`main`) and `docs/vision.md`; every decision about scope, changes and design is
made inside the OpenSpec workflow.

Flow: **install → project context → explore → per change: proposal → specs → design → tasks → apply → verify →
archive → change requests**

## Quick run sheet (from zero)

Short list of every command in order; details for each step are further down.

### 0. Install (once)

`docs/vision.md` is already on this branch.

| Command                                      | What it does                                              |
|----------------------------------------------|-----------------------------------------------------------|
| `npm install -g @fission-ai/openspec@latest` | installs the `openspec` CLI on your machine               |
| `openspec --version`                         | optional: note the version                                |
| `openspec init --tools claude`               | creates `openspec/` and the `/opsx:*` commands for Claude Code |
| `openspec config profile`                    | choose the expanded workflow (adds `/opsx:new`, `/opsx:continue` …) |
| `openspec update`                            | rewrites the `/opsx:*` files in this repo for that profile |

Commit: "Install OpenSpec \<version\>"

### 1. Project context (once)

No OpenSpec command: edit `openspec/config.yaml` by hand (context + rules, see step 1). Check every line against
`pom.xml`, `src/` and `docs/vision.md`; nothing validates the file.

Commit: "Add OpenSpec project context"

### 2. Plan the changes (once)

```
/opsx:explore "Build the Hotel Reservation System described in docs/vision.md. Which changes, in which order?"
```

### 3–5. For each change from step 2

`<change>` is the change name. One session per change, `/clear` in between.

**3. Plan**

| Command                               | What it does                                          |
|---------------------------------------|-------------------------------------------------------|
| `/opsx:propose <change>`              | writes proposal, specs, design and tasks in one go    |
| `openspec status --change <change>`   | shows which documents are done                        |
| `openspec validate <change> --strict` | checks the documents                                  |

Step by step instead: `/opsx:new <change>`, then `/opsx:continue <change>` until all documents exist (see step 3).

Commit: "Plan change \<change\>"

**4. Implement**

| Command                | What it does                               |
|------------------------|--------------------------------------------|
| `/opsx:apply <change>` | works through `tasks.md`, ticks the boxes  |

**5. Verify, merge request, archive**

| Command                  | What it does                                            |
|--------------------------|---------------------------------------------------------|
| `./mvnw verify`          | runs all tests                                          |
| `/opsx:verify <change>`  | checks the code against the change documents            |

Commit: "Implement change \<change\> with tests", then open the merge request.

Review fixes: `/opsx:update <change>` if behavior changes → fix code → `./mvnw verify` → `/opsx:verify <change>` → push.

| Command                  | What it does                                            |
|--------------------------|---------------------------------------------------------|
| `/opsx:archive <change>` | only after the merge request is approved                |

Commit: "Archive change \<change\>", push, merge.

### 6. Change requests

Same loop as 3–5, but start from the request text:

```
/opsx:explore "<request text>"
```

### Run the app

```bash
./mvnw spring-boot:run
```

Then open <http://localhost:8080>.

## How OpenSpec works

- `openspec/specs/<capability>/spec.md` is the **current truth** of the system, grouped by capability. It starts empty.
- Every piece of work is a **change** in `openspec/changes/<change-name>/` with `proposal.md`, `design.md`,
  `tasks.md` and **delta specs** (`specs/<capability>/spec.md` with `## ADDED / MODIFIED / REMOVED Requirements`).
- `/opsx:archive` merges the delta specs into `openspec/specs/` and moves the change to `openspec/changes/archive/`.
- Requirements are written as `### Requirement:` with `SHALL`/`MUST`, each with `#### Scenario:` blocks
  (WHEN / THEN).
- **Greenfield works the same way.** All work is a change, even the very first feature. On a new project
  `openspec/specs/` is empty, so the first changes contain only `## ADDED Requirements`; `/opsx:archive` then writes
  the first specs into `openspec/specs/`. Building the whole system is a series of changes. One single change for
  the whole vision would also work, but it would be far too big to review and to fit into one session.
- There is no batch command that creates every spec at once. You do one change at a time, which keeps reviews small
  and token use under control.

## Progress

| Step | Name                               | Command                                              | Status |
|------|------------------------------------|------------------------------------------------------|--------|
| 0    | Install                            | `openspec init --tools claude`                       | Done   |
| 1    | Project context (= steering)       | edit `openspec/config.yaml`                          | Done   |
| 2    | Explore and plan the changes       | `/opsx:explore`                                      | Done   |
| 3    | Per change: plan                   | `/opsx:propose` (or `/opsx:new`, `/opsx:continue` ×4) | Open   |
| 4    | Per change: implement              | `/opsx:apply`                                        | Open   |
| 5    | Per change: verify, MR, archive    | `./mvnw verify`, `/opsx:verify`, `/opsx:archive`     | Open   |
| 6    | Change requests                    | `/opsx:explore`, `/opsx:propose` … `/opsx:archive`   | Open   |

---

## Step 0 – Install

The branch `hotel-reservation-openspec` starts from `main`; `docs/vision.md` is already added.

```bash
npm install -g @fission-ai/openspec@latest           # needs Node >= 22.12 (installed: v24)
openspec --version                                   # used here: 1.16.0
openspec init --tools claude                         # creates openspec/ + the /opsx:* commands for Claude Code
openspec config profile                              # choose the expanded workflow (new, continue, ff, verify …)
openspec update                                      # regenerates the /opsx:* files for the chosen profile
```

- `init` sets up OpenSpec in this repo: the `openspec/` folder (`specs/`, `changes/`, `config.yaml`) and the
  Claude Code slash commands. `--tools claude` skips the tool picker; without it, `init` asks in a terminal and
  fails when no terminal is available and no tool folder (like `.claude/`) exists yet.
- The default profile only has `/opsx:propose`, `/opsx:explore`, `/opsx:apply` and a few more. `config profile`
  switches to the expanded set, and `update` rewrites the slash command files in the repo to match.
- `config profile` needs a real terminal (it fails under Claude Code's `!` prefix). Choose "Workflows only" (keep
  delivery `both`) and add New change, Continue change, Fast-forward and Verify change to the six core workflows.
  The setting is global (`openspec config list`), not per repo.

Check what `init` created (`openspec/`, `.claude/commands/opsx/` and/or `.claude/skills/openspec-*`) and commit it
as "Install OpenSpec <version>".

Other globally installed Claude Code plugins can react to words like "write requirements" or "implement". Always
call the `/opsx:*` commands by name, or turn those plugins off for this branch (`/plugin`).

## Step 1 – Project context (once)

OpenSpec has no steering command. Everything project-wide goes into `openspec/config.yaml`; `context` is sent with
every artifact, `apply` and `archive`, `rules` only with the artifact they name. Keep it short: OpenSpec warns that
long context makes the output worse.

The result for this project is [`openspec/config.yaml`](../openspec/config.yaml). It has these parts:

| Key          | Sent with                                   | Put in it                                             |
|--------------|---------------------------------------------|-------------------------------------------------------|
| `schema`     | –                                           | keep `spec-driven` from `init`                        |
| `context`    | every artifact, `apply`, `archive`          | facts the agent cannot read from the code             |
| `rules.<id>` | only the artifact `<id>` (`proposal`, `specs`, `design`, `tasks`) | checks that artifact must meet  |
| `operations` | `apply` / `archive` (new in 1.16, optional) | advice on how to work, not what to produce            |

There is no OpenSpec command for this step, and nothing validates the file: `openspec validate` only checks changes
and specs, and `openspec instructions` needs a change. Tips:

- **Only facts, no decisions.** Put in what the repo and the vision already fix (stack, versions, existing classes,
  build commands). Leave architecture and design choices to the changes, where they get reviewed.
- **Check every line against the repo, not against a plan.** A first draft listed Spring Security, but it is not in
  `pom.xml`; the agent would have treated it as present. State what is missing instead.
- **Use the vision's words.** A first draft said "Housekeeper", the vision says "Housekeeping"; the context is the
  glossary OpenSpec does not have.
- **Name what already exists.** Point to existing base classes (`HrsBrowserlessTest`, `PlaywrightIT`) and the
  generated jOOQ package, or the agent writes its own.
- **Exact versions from `pom.xml`**, not "Spring Boot 4".
- **Keep it short** (about 10 lines); put per-artifact checks in `rules`, not in `context`.
- **`rules` keys must be artifact ids** of the schema (`openspec templates` lists them); an unknown key is only a
  warning, so a typo silently does nothing.
- **See it in use** from step 3 on: `openspec instructions specs --change <name>` prints the prompt with your context
  and rules in it. Check this once for the first change.

Commit: "Add OpenSpec project context". This file has the biggest effect on the result.

## Step 2 – Explore and plan the changes

```
/opsx:explore "Build the Hotel Reservation System described in docs/vision.md. Which changes, in which order,
               and which capabilities should the specs be grouped into? Do not write any files."
```

`explore` only thinks and discusses, it writes nothing. Let OpenSpec propose the split and the order, and answer the
open questions it raises from the vision. Steer only if it suggests one huge change.

Agreed changes:

| # | Change name                  | Capabilities                   | Status |
|---|------------------------------|--------------------------------|--------|
| 1 | `add-staff-login`            | `staff-access`                 | Open   |
| 2 | `add-room-management`        | `room-management`              | Open   |
| 3 | `add-reception-reservations` | `reservations`, `availability` | Open   |
| 4 | `add-online-booking`         | `online-booking`               | Open   |
| 5 | `add-housekeeping`           | `housekeeping`                 | Open   |
| 6 | `add-check-in-out`           | `check-in-out`                 | Open   |
| 7 | `add-reports`                | `reports`                      | Open   |

## Step 3 – Per change: plan

A change has four documents that build on each other: proposal → specs → design → tasks. There are two ways to
write them:

|            | `/opsx:propose <change>`                        | `/opsx:new <change>` + `/opsx:continue <change>` ×4 |
|------------|-------------------------------------------------|-----------------------------------------------------|
| Writes     | change folder **and all four documents** in one run | empty folder first, then one document per call  |
| Review     | once, at the end                                | after every document                                |
| Best when  | the change was already worked out in `/opsx:explore` | the change is still unclear, or you want to stop between documents |
| Profile    | default and expanded                            | expanded only                                       |

**Prefer `/opsx:propose`**, especially after an `/opsx:explore` session: it uses the decisions made in that
conversation and writes all four documents, which saves four calls. Both ways produce the same files, and nothing
is implemented before `/opsx:apply`. (`/opsx:ff <change>` does the same for a change already created with
`/opsx:new`.)

### Step by step instead

`/opsx:new` creates only the empty folder `openspec/changes/<change>/`. Each `/opsx:continue` then writes **the next
missing document and stops**, so you review and fix it before the next one is written. Four documents, so four
calls:

| Call | Command                    | Writes                         | What to review                                |
|------|----------------------------|--------------------------------|-----------------------------------------------|
| 1    | `/opsx:new <change>`       | empty change folder            | the change name                               |
| 2    | `/opsx:continue <change>`  | `proposal.md`                  | why, what, which capabilities are affected    |
| 3    | `/opsx:continue <change>`  | `specs/<capability>/spec.md`   | requirements + scenarios (review carefully)   |
| 4    | `/opsx:continue <change>`  | `design.md`                    | tables, packages, views                       |
| 5    | `/opsx:continue <change>`  | `tasks.md`                     | checklist, including test tasks               |

You don't count the calls yourself. The list of documents comes from the schema in `openspec/config.yaml`
(`spec-driven`: proposal, specs, design, tasks). After each call, `/opsx:continue` reports what it wrote and what is
next; `openspec status --change <change>` shows which documents are done and which are still missing. When all are
done, the change is ready for `/opsx:apply`. An extra `/opsx:continue` does no harm; it just reports that nothing is
left.

### Check the result (both ways)

```bash
openspec validate <change> --strict
openspec status --change <change>
```

- After `/opsx:propose`, review the documents in order (proposal → specs → design → tasks): a mistake in an early
  document spreads into the later ones. If you fix something by hand, run `/opsx:update <change>` so the other
  documents stay consistent.
- Check that every rule and error case from the vision became a `#### Scenario:`.
- Commit the planning artifacts after review: "Plan change <change>".

## Step 4 – Per change: implement

```
/opsx:apply <change>
```

Works through `tasks.md` and ticks the boxes. There is no separate reviewer per task, so check the result yourself
in step 5.

**Token budget:** run one change per session and `/clear` between changes. If the limit runs out, `/opsx:apply`
picks up at the first unticked task.

## Step 5 – Per change: verify, merge request, archive

```
/opsx:verify <change>        → does the code match proposal, specs, design and tasks? (changes no files)
/opsx:archive <change>       → merges delta specs into openspec/specs/, moves the change to archive/
```

**When to run them in a merge request flow:** verify before the merge request and after every review fix; archive
once, as the last commit of the same merge request, after it is approved.

| When                          | Do                                                                                   |
|-------------------------------|--------------------------------------------------------------------------------------|
| Implementation done           | `./mvnw verify`, `/opsx:verify <change>`, fix findings                               |
| Before opening the MR         | commit "Implement change <change> with tests", open the merge request                |
| Review comments               | behavior changes → `/opsx:update <change>` first; fix code; `./mvnw verify`; `/opsx:verify <change>`; push |
| MR approved                   | `/opsx:archive <change>`, commit "Archive change <change>", push, merge              |

Why archive inside the merge request and not after the merge:

- `main` gets the code and the updated `openspec/specs/` in one merge, so they never disagree. Archiving after the
  merge leaves `main` with code that the main specs don't describe yet.
- Reviewers can check the merged spec text too.
- No second merge request just for the archive.

If the team does not allow pushes after approval, archive just before opening the merge request instead. Review
fixes that change behavior must then be made in `openspec/specs/` directly, because the change is already archived.

```bash
openspec list --specs        # the capabilities now in the main specs
openspec view                # dashboard
```

**Repeat steps 3–5 for every change** from step 2, in order. Then run the app with `./mvnw spring-boot:run` and open
<http://localhost:8080>.

## Step 6 – Change requests

A change request changes behavior that is already in `openspec/specs/`. Start from the request text only, so
OpenSpec has to find the affected requirements itself:

```
/opsx:explore "<request text>"   → which requirements in openspec/specs/ change? open questions?
/opsx:propose <change>           → delta specs should be MODIFIED, not ADDED
                                   (or /opsx:new + /opsx:continue ×4, see step 3)
/opsx:apply <change>
/opsx:verify <change>            → then merge request and review as in step 5
/opsx:archive <change>           → after the merge request is approved
```

Answer the stakeholder questions OpenSpec raises; do not hand it decisions it did not ask for.

This is OpenSpec's main strength (brownfield deltas), so review the MODIFIED requirements closely.
