# cc-sdd Workflow: Hotel Reservation System (PoC)

Plan for rebuilding the Hotel Reservation System with [cc-sdd](https://github.com/gotalab/cc-sdd) (v3.1.0,
Claude Code skills mode), so it can be compared with the AIUP build on the branch `hotel-reservation`
(see `aiup-workflow.md`).

Flow: **steering → discovery → spec batch → per spec: requirements → design → tasks → implementation → validation**

AIUP keeps one project-wide set of documents (requirements, entity model, use cases) and implements one use case at a
time. cc-sdd works one spec per feature, each in `.kiro/specs/<feature>/` with its own `requirements.md` (EARS),
`design.md` and `tasks.md`. `/kiro-impl` works through the tasks on its own. cc-sdd has no stack plugins: the Vaadin,
jOOQ, Flyway, Karibu and Playwright conventions go into steering files.

## Progress

| Step | Name                         | Command                                         | Status |
|------|------------------------------|-------------------------------------------------|--------|
| 0    | Branch and install           | `npx cc-sdd@latest --claude-skills --lang en`   | Open   |
| 1    | Steering                     | `/kiro-steering`, `/kiro-steering-custom`       | Open   |
| 2    | Discovery                    | `/kiro-discovery`                               | Open   |
| 3    | Spec batch                   | `/kiro-spec-batch`                              | Open   |
| 4    | Requirements, design, tasks  | `/kiro-spec-requirements` … `/kiro-spec-tasks`  | Open   |
| 5    | Implementation               | `/kiro-impl <feature>`                          | Open   |
| 6    | Validation                   | `/kiro-validate-impl <feature>`                 | Open   |
| 7    | Change requests              | `/kiro-discovery "<CR text>"`                   | Open   |
| 8    | Comparison with AIUP         | by hand                                         | Open   |

---

## How AIUP steps map to cc-sdd

| AIUP (hotel-reservation branch)     | cc-sdd                                                              | Output                                         |
|-------------------------------------|---------------------------------------------------------------------|------------------------------------------------|
| Install `aiup-vaadin-jooq`          | `npx cc-sdd@latest --claude-skills --lang en`                       | `.claude/skills/kiro-*`, `.kiro/settings/`     |
| `docs/vision.md` (by hand)          | Same file, used as input to `/kiro-discovery`                       | `brief.md`, `roadmap.md`                       |
| Stack knowledge from the plugin     | `/kiro-steering` + `/kiro-steering-custom`                          | `.kiro/steering/*.md`                          |
| `/requirements`, glossary           | Requirements per feature in EARS form ("WHEN … THEN … SHALL …")     | `.kiro/specs/<feature>/requirements.md`        |
| `/entity-model`, `/use-case-diagram`| No equivalent; the data model goes in the foundation spec's design | `design.md`                                    |
| `/use-case-spec UC-XXX`             | No equivalent; flows and business rules become acceptance criteria  | `requirements.md`                              |
| `/spec-review`                      | `/kiro-validate-design`, cross-spec review in `/kiro-spec-batch`    | review report                                  |
| `/flyway-migration`                 | A task in the foundation spec                                       | `tasks.md`                                     |
| `/implement UC-XXX`                 | `/kiro-impl <feature>` (test first, separate reviewer, per task)    | code, ticked-off `tasks.md`                    |
| `/karibu-test`, `/playwright-test`  | Written inside `/kiro-impl`, only if steering requires them         | tests                                          |
| `/coverage-check UC-XXX`            | `/kiro-validate-impl <feature>`                                     | GO / NO-GO / MANUAL_VERIFY_REQUIRED            |
| Status lines in the docs            | `/kiro-spec-status <feature>`, approval flags in `spec.json`        |                                                |
| `docs/change_requests/CR-00x`       | `/kiro-discovery "<CR text>"` routes to the existing spec           | updated spec                                   |

## Step 0 – Branch and install

```bash
git switch main && git switch -c cc-sdd-poc
git checkout hotel-reservation -- docs/vision.md
npx cc-sdd@latest --claude-skills --lang en --dry-run   # preview first
npx cc-sdd@latest --claude-skills --lang en
```

Start from `main` and copy only the vision, so cc-sdd does not start from the AIUP specs.

The AIUP plugins are installed globally and can catch requests like "write requirements". Always call the
`/kiro-*` skills by name, or turn off the AIUP plugins for this branch.

## Step 1 – Steering (once)

```
/kiro-steering
/kiro-steering-custom vaadin-jooq
/kiro-steering-custom testing
/kiro-steering-custom domain
```

**Output:** `.kiro/steering/product.md`, `tech.md`, `structure.md`, plus the custom files.

- **vaadin-jooq:** Vaadin 25 Flow views, jOOQ for data access, Flyway migrations are never edited (add a new one),
  `./mvnw compile` runs Flyway against H2 and generates the jOOQ classes.
- **testing:** Karibu tests for every view (`./mvnw test`), a Playwright `*IT` for every feature (`./mvnw verify`),
  test-data ID ranges.
- **domain:** the glossary (roles, terms, words to avoid).

This step replaces the AIUP stack plugin and has the biggest effect on the result.

## Step 2 – Discovery

```
/kiro-discovery "Build the Hotel Reservation System described in docs/vision.md"
```

**Output:** `brief.md` and `roadmap.md`. For a system this size it should suggest several specs. Steer it toward the
package layout, for example: `foundation` (schema, login, staff), `guest-booking`, `front-desk-reservations`,
`reception`, `housekeeping`, `management`, `reports`.

## Step 3 – Spec batch

```
/kiro-spec-batch
```

Creates all specs from the roadmap in dependency order and checks them against each other (contradictions,
duplicated responsibilities, interface mismatches).

## Step 4 – Requirements, design, tasks (per spec, human approval at each step)

```
/kiro-spec-requirements foundation   → review the EARS requirements
/kiro-validate-gap foundation        → optional; useful once code exists
/kiro-spec-design foundation         → review the data model, file plan and boundaries
/kiro-validate-design foundation
/kiro-spec-tasks foundation          → review tasks.md (boundary, dependencies, requirement IDs per task)
```

Do not use `-y` (auto-approve): the approval steps are where the PoC is judged. Check that the alternative flows and
business rules from the vision ended up as acceptance criteria.

## Step 5 – Implementation

```
/kiro-impl foundation
```

Runs one task at a time: implementer, then reviewer, then debugger if stuck (at most 2 debug rounds per task).
Do `foundation` first because it owns the schema, then the other specs in dependency order.

## Step 6 – Validation

```bash
./mvnw verify
```
```
/kiro-validate-impl foundation
/kiro-spec-status foundation
```

**Repeat steps 4–6 for every spec**, then run the app with `./mvnw spring-boot:run` and open
<http://localhost:8080>.

## Step 7 – Change requests

Replay the AIUP change requests from `hotel-reservation:docs/change_requests/`:

```
/kiro-discovery "<text of CR-001>"    → should route to guest-booking
/kiro-spec-requirements guest-booking
/kiro-spec-design guest-booking
/kiro-spec-tasks guest-booking
/kiro-impl guest-booking
/kiro-validate-impl guest-booking
```

Then the same for CR-002 (seasonal rates).

## Step 8 – Comparison with AIUP

- Human corrections needed at each approval step, compared with the AIUP review edits.
- Whether all 25 AIUP use cases' business rules are covered. Run AIUP's `/coverage-check` against the cc-sdd code as
  a neutral check.
- Number of tests, `./mvnw verify` result, time and token cost.
- Traceability: AIUP marks code with `UC-XXX BR-YYY`; cc-sdd links tasks to requirement numbers (1.1, 1.2), which do
  not appear in the code unless steering asks for markers.
- Effort to apply CR-001 and CR-002.
