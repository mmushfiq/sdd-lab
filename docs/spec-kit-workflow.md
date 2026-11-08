# Spec Kit Workflow: Hotel Reservation System

How to build the Hotel Reservation System described in `docs/vision.md` with
[Spec Kit](https://github.com/github/spec-kit) (CLI `specify-cli`, Claude Code skills `/speckit-*`).
The only inputs are this repository (`main`) and `docs/vision.md`; every decision about scope, user stories and
design is made inside the Spec Kit workflow.

Flow: **install → constitution → per feature: specify → clarify → plan → checklist → tasks → analyze →
implement → converge → change requests**

## Quick run sheet (from zero)

Short list of every command in order; details for each step are further down.

### 0. Install (once)

| Command                                                         | What it does                                             |
|-----------------------------------------------------------------|----------------------------------------------------------|
| `git checkout hotel-reservation-openspec -- docs/vision.md`     | the same vision file the other tools got                 |
| `uv tool install specify-cli`                                   | installs the `specify` CLI (uv brings Python 3.11+)      |
| `specify version`                                               | note the version                                         |
| `specify init --here --force --non-interactive --integration claude --integration-options="--no-model-invocation"` | creates `.specify/` and the `/speckit-*` skills in `.claude/skills/` |

Commits: "Add product vision for Hotel Reservation System", "Install Spec Kit \<version\>"

### 1. Constitution (once)

```
/speckit-constitution <facts about the project, see step 1>
```

Commit: "Add Spec Kit constitution"

### 2–6. For each feature

`<feature>` is the folder `specs/NNN-<name>/` that `/speckit-specify` creates.

| Step | Command                  | Writes                                   | Commit                                       |
|------|--------------------------|------------------------------------------|----------------------------------------------|
| 2    | `/speckit-specify <what and why>` | `spec.md`, `checklists/requirements.md` | "Specify feature \<feature\>"           |
| 2    | `/speckit-clarify`       | answers into `spec.md` (≤ 5 questions per run) | "Clarify feature \<feature\>"          |
| 3    | `/speckit-plan <tech stack>` | `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md` | "Plan feature \<feature\>" |
| 3    | `/speckit-checklist`     | `checklists/<topic>.md` (optional)       | with the plan commit                         |
| 4    | `/speckit-tasks`         | `tasks.md`                               | "Generate tasks for feature \<feature\>"     |
| 4    | `/speckit-analyze`       | nothing (read-only report)               | –                                            |
| 5    | `/speckit-implement <scope>` | code, ticks in `tasks.md`            | "Implement \<user story\> of feature \<feature\> with tests" |
| 6    | `./mvnw verify`, `/speckit-converge` | appends missing tasks to `tasks.md` or reports `Converged` | "Converge feature \<feature\>" |

Repeat 5 → 6 until converge reports **Converged**.

### 7. Change requests

Same loop as 2–6, started from the request text only: `/speckit-specify "<request text>"`.

### Run the app

```bash
./mvnw spring-boot:run
```

Then open <http://localhost:8080>.

## How Spec Kit works

- `.specify/memory/constitution.md` is the **project-wide rule book**. Every later step is checked against it
  (`plan.md` has a "Constitution Check" gate).
- Every piece of work is a **feature** in `specs/NNN-<name>/`. `/speckit-specify` creates the folder and
  `spec.md`: user stories with priorities (P1, P2 …), acceptance scenarios (Given / When / Then), functional
  requirements `FR-001 …`, key entities and success criteria. Open points are marked `[NEEDS CLARIFICATION]`.
- `spec.md` is the **what and why** (no tech stack); `plan.md` is the **how** (stack, structure, data model).
- `tasks.md` has phases: Setup, Foundational, **one phase per user story** in priority order, Polish. `[P]` marks
  tasks that can run in parallel. Each user story is meant to be testable on its own (MVP = P1 story).
- clarify, checklist and analyze are **optional quality gates**; only specify → plan → tasks → implement are
  required. converge closes the loop after implement: it only appends tasks, it never edits code.
- **No "current truth" merge.** Unlike OpenSpec's archive, Spec Kit does not merge features into one main spec.
  Each feature folder stays as it is; what happens to it after delivery is the team's choice
  ([Persisting specifications](https://github.github.io/spec-kit/how-to/persisting-specs.html)). Only the
  constitution is shared by all features.
- Feature branches (`001-<name>`) come from the optional **git** extension. This PoC stays on one branch, so it is
  not installed.

## Progress

| Step | Name                              | Command                                        | Status |
|------|-----------------------------------|------------------------------------------------|--------|
| 0    | Install                           | `specify init --here --integration claude`     | Done   |
| 1    | Constitution                      | `/speckit-constitution`                        | Done   |
| 2    | Per feature: specify, clarify     | `/speckit-specify`, `/speckit-clarify`         | Done   |
| 3    | Per feature: plan, checklist      | `/speckit-plan`, `/speckit-checklist`          | Done   |
| 4    | Per feature: tasks, analyze       | `/speckit-tasks`, `/speckit-analyze`           | Open   |
| 5    | Per feature: implement            | `/speckit-implement`                           | Open   |
| 6    | Per feature: verify, converge     | `./mvnw verify`, `/speckit-converge`           | Open   |
| 7    | Change requests                   | `/speckit-specify` … `/speckit-converge`       | Open   |

---

## Step 0 – Install

The branch `hotel-reservation-spec-kit` starts from `main`. First add the vision, the same file the other branches
used (commit a4e53cd, it also adds `.DS_Store` to `.gitignore`):

```bash
git checkout hotel-reservation-openspec -- docs/vision.md .gitignore
```

Then install and initialise:

```bash
uv tool install specify-cli          # PyPI package; the system Python is 3.9, uv used 3.12 itself
specify version                      # used here: 1.3.2
specify init --here --force --non-interactive --integration claude --integration-options="--no-model-invocation"
```

- `--here` uses the current directory, `--force` allows a non-empty one, `--non-interactive` stops it from waiting
  for a menu choice (needed when Claude Code runs it). It adds only new files, no existing file changes:
  - `.claude/skills/speckit-*/SKILL.md`: 9 skills (analyze, checklist, clarify, constitution, converge,
    implement, plan, specify, tasks);
  - `.specify/memory/constitution.md` (still the empty template), `.specify/templates/` (spec, plan, tasks,
    checklist, constitution), `.specify/scripts/bash/` (feature folder and prerequisite scripts),
    `.specify/workflows/speckit/workflow.yml` (an optional automated specify → implement run, not used here);
  - `.specify/init-options.json` (`feature_numbering: sequential`, so features are `specs/001-…`),
    `.specify/integration.json`, `.specify/integrations/*.manifest.json` and a `.specify/.gitignore`.
- `--no-model-invocation` makes the core skills **user-invocable only**: Claude runs them only when you type
  `/speckit-<command>`. Other globally installed plugins also react to words like "specify" or "implement"; this
  keeps every step under your control and keeps the skill descriptions out of the context.
- The old syntax (`--ai claude`, `/speckit.specify`) is from versions before 1.0. Spec Kit's docs still write
  commands as `/speckit.*`; in Claude Code they are `/speckit-*`.
- `init` prints a warning to add `.claude/` to `.gitignore` because of credentials. Here `.claude/` only holds the
  skills (commit them); the local `.claude/settings.local.json` is already ignored by the global git ignore.

Commit everything `init` created as "Install Spec Kit \<version\>".

## Step 1 – Constitution (once)

The constitution is Spec Kit's version of OpenSpec's `config.yaml` context, but stronger: `plan.md` must pass a
"Constitution Check" against it. Pass the principles as arguments:

```
/speckit-constitution <stack, versions, existing classes, test rules, language of the vision>
```

Tips (the same lessons as the OpenSpec step 1):

- **Only facts and agreed rules, no design.** Put in what `pom.xml`, `src/` and the vision already fix. Spec Kit's
  docs warn: "Do not invent standards merely to fill the constitution template."
- **Check every line against the repo.** Exact versions from `pom.xml`; say what is *not* there (for example
  Spring Security), or the agent treats it as present.
- **Name what already exists**: `core/ui/HrsBrowserlessTest`, `core/ui/PlaywrightIT`, the generated jOOQ package,
  `./mvnw verify` as the test command.
- **Use the vision's words** (roles, statuses), since Spec Kit has no glossary either.
- The template asks for principles plus a governance section with a version. Review that it did not add rules
  you did not give it (library-first, CLI per feature …: these come from the template's examples).

Commit: "Add Spec Kit constitution". Like OpenSpec's context, this file has the biggest effect on the result.

## Step 2 – Specify and clarify

Spec Kit has **no explore command** to split the vision into changes. `/speckit-specify` writes one feature with
several prioritised user stories, and `tasks.md` later gets one phase per user story. Two ways for this project:

|            | A: one feature for the whole vision (recommended)    | B: one feature per area                          |
|------------|------------------------------------------------------|--------------------------------------------------|
| Command    | `/speckit-specify` once with the vision              | `/speckit-specify` several times, one area each  |
| Who splits | Spec Kit (user stories P1, P2 …)                     | you decide the areas before Spec Kit runs        |
| Size       | one big spec; implement it per user story            | small specs, but the split is not the tool's     |

**A** keeps the split inside the tool (fair against OpenSpec's explore) and is how Spec Kit's quickstart starts a
greenfield app. The size is handled in step 5 by implementing one user story per run.

```
/speckit-specify Build the Hotel Reservation System described in docs/vision.md.
```

Then review `spec.md`:

- every rule and error case from the vision is an acceptance scenario or an `FR-`;
- the user stories and their priorities make sense as an order to build in;
- `checklists/requirements.md` (written by specify) shows what is still open.

```
/speckit-clarify                       → up to 5 questions, answers go into spec.md
/speckit-clarify Focus on <area>       → run again for the next area until nothing is open
```

Answer only what the vision leaves open; don't hand it decisions it did not ask for.

Commits: "Specify feature \<feature\>", "Clarify feature \<feature\>".

## Step 3 – Plan (and checklist)

The tech stack goes here, not into the spec:

```
/speckit-plan Use the existing Spring Boot, Vaadin Flow, jOOQ and Flyway setup from pom.xml; tests as in the constitution.
```

Writes `plan.md` (technical context, constitution check, project structure), `research.md` (decisions),
`data-model.md`, `contracts/` and `quickstart.md`. Review that it reuses the existing setup and does not add new
dependencies or layers.

Optional: `/speckit-checklist` (or `/speckit-checklist Focus on <area>`) writes "unit tests for the requirements".
You, not the agent, tick its items; `/speckit-implement` asks before it starts when items are unticked. Gaps found
here go back to `/speckit-clarify`.

Commit: "Plan feature \<feature\>".

## Step 4 – Tasks and analyze

```
/speckit-tasks        → tasks.md: Setup, Foundational, one phase per user story, Polish
/speckit-analyze      → read-only report: tasks without a requirement, plan vs. spec conflicts, gaps
```

Ask for tests in `/speckit-tasks` if the constitution does not already require them (tests are only generated
"when requested"). Fix findings at the step that owns them (specify/clarify, plan or tasks) and run analyze again
until it is clean.

Commit: "Generate tasks for feature \<feature\>".

## Step 5 – Implement

For a big feature, work in stages (Spec Kit's own advice) and `/clear` between them:

```
/speckit-implement Implement only the Setup and Foundational phases. Stop before the user stories.
/speckit-implement Implement user story 1.
/speckit-implement Implement user story 2.
...
```

It works through `tasks.md` in order and ticks the boxes. Run `./mvnw verify` after each stage.

**Token budget:** one stage per session. If the limit runs out, the next run picks up at the first unticked task.

Commit per stage: "Implement \<user story\> of feature \<feature\> with tests".

## Step 6 – Verify and converge

```bash
./mvnw verify
```

```
/speckit-converge
```

`converge` checks the code against `spec.md`, `plan.md` and `tasks.md`. It never edits code; it either reports
**Converged** (tasks.md unchanged) or appends the missing work as tasks under a "Convergence" section. Then run
`/speckit-implement` and `/speckit-converge` again until it reports Converged. Run it after every `implement` on the
current `tasks.md`, not before.

Commit: "Converge feature \<feature\>".

## Step 7 – Change requests

A change request changes behavior that is already specified. Spec Kit has no delta specs (no ADDED / MODIFIED),
so either revise the existing feature or write a new feature folder. Use a **new feature folder per change
request**, started from the request text only, so Spec Kit has to find the affected behavior itself:

```
/speckit-specify "<request text>"      → specs/NNN-<name>/spec.md
/speckit-clarify
/speckit-plan
/speckit-tasks
/speckit-analyze
/speckit-implement
/speckit-converge                      → repeat implement/converge until Converged
```

Use the same two change requests as the other branches, word for word. Check that the new spec names the
existing requirements it changes, and decide whether the old feature's `spec.md` must be updated so it does not
contradict the change (Spec Kit leaves this to you).
