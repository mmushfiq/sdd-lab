# AIUP Workflow: Hotel Reservation System

Step-by-step plan for building the Hotel Reservation System with the AI Unified Process (AIUP).

Flow: **vision → requirements → entity model → use case diagram → use case specs → (business process / test cases)
→ spec review → database → implementation → tests → coverage check**

Every step writes plain files under `docs/` (or `src/`) that are reviewed and committed before moving on.

## Progress

| Step | Name                              | Command                                  | Status  |
|------|-----------------------------------|------------------------------------------|---------|
| 0    | Install stack plugin              | `/plugin install aiup-vaadin-jooq`       | Done    |
| 1    | Vision                            | written by hand                          | Done    |
| 2    | Requirements and glossary         | `/requirements`                          | Done    |
| 3    | Entity model                      | `/entity-model`                          | Done    |
| 4    | Use case diagram                  | `/use-case-diagram`                      | Done    |
| 5    | Use case specifications           | `/use-case-spec UC-XXX`                  | Done    |
| 6    | Business process and test cases   | `/business-process`, `/test-case BP-XXX` | Open    |
| 7    | Spec review                       | `/spec-review`                           | Done    |
| 8    | Database migration                | `/flyway-migration`                      | Done    |
| 9    | Implementation                    | `/implement UC-XXX`                      | Open    |
| 10   | Tests                             | `/karibu-test`, `/playwright-test`       | Open    |
| 11   | Coverage check                    | `/coverage-check UC-XXX`                 | Open    |

---

## Step 0 – Install the stack plugin (once)

```
/plugin install aiup-vaadin-jooq
```

`aiup-core` (the specification skills) is already installed. `aiup-vaadin-jooq` adds the construction skills for this
repo's stack: Vaadin 25, jOOQ, Flyway, H2, Karibu and Playwright.

Work happens on the branch `hotel-reservation`.

## Step 1 – Vision (written by hand)

**Output:** `docs/vision.md`

Mission, target users (Guest, Receptionist, Manager, Housekeeping), goals, scope, constraints and success measures.
Every later step reads this file.

## Step 2 – Inception: requirements

```
/requirements
```

**Output:** `docs/requirements.md` (user stories FR-xxx, quality requirements NFR-xxx, constraints C-xxx) and
`docs/glossary.md` (one name per domain concept, plus words to avoid).

Review carefully: this is the cheapest place to fix misunderstandings.

## Step 3 – Elaboration: entity model

```
/entity-model
```

**Output:** `docs/entity_model.md` with a Mermaid ER diagram and attribute tables (data types, validation rules).
Expected entities: Room, RoomType, Guest, Reservation, StaffMember, RoomBlock.

## Step 4 – Use case diagram

```
/use-case-diagram
```

**Output:** `docs/use_cases.puml`, a PlantUML diagram mapping the four actors to their use cases (e.g. Search
Availability, Reserve Room Type, Cancel Reservation, Check In Guest, Check Out Guest, Mark Room Clean, Maintain
Rooms, View Occupancy Report). The use case IDs (UC-001 …) are assigned by the tool.

## Step 5 – Use case specifications (one per use case)

```
/use-case-spec UC-001
/use-case-spec UC-002
...
```

**Output:** `docs/use_cases/UC-XXX-*.md`, each with actors, preconditions, main success scenario, alternative flows
(room type fully reserved, no clean room available, cancellation too late …), postconditions and business rules.

## Step 6 – Optional: business process and end-to-end test cases

```
/business-process        → docs/processes/BP-001-*.bpmn
/test-case BP-001        → docs/test_cases/TC-*.md
```

A good fit here: the guest journey (reserve → check in → check out → clean room) crosses all four roles. The test
case generates one end-to-end journey per path through the process.

## Step 7 – Spec review (quality gate before coding)

```
/spec-review
```

Checks the documents against each other: requirements with no use case, contradicting rules, ambiguous wording,
words from the glossary's Avoid column. Ask for a traceability matrix if needed. Fix the findings and run it again.

## Step 8 – Construction: database

```
/flyway-migration
```

**Output:** `src/main/resources/db/migration/V*.sql`. Then run:

```bash
./mvnw compile
```

This runs Flyway against H2 and generates the jOOQ classes.

## Step 9 – Implement, one use case at a time

```
/implement UC-001
```

**Output:** Vaadin views, forms and grids plus jOOQ data access for that use case.

## Step 10 – Tests per use case

```
/karibu-test UC-001        → server-side UI unit tests        (./mvnw test)
/playwright-test UC-001    → browser integration test (*IT)   (./mvnw verify)
/playwright-test TC-001    → end-to-end journey from Step 6
```

## Step 11 – Coverage check

```
/coverage-check UC-001
```

Reports which main-scenario steps, alternative flows and business rules of the spec have no code or no test behind
them.

**Repeat steps 9–11 for every use case**, then run the app:

```bash
./mvnw spring-boot:run
```

and open <http://localhost:8080>.

---

## Handling changes later

1. Update the specification first (`/requirements`, `/use-case-spec UC-XXX`, `/entity-model`).
2. Run `/spec-review` again.
3. Run `/flyway-migration` again if the entity model changed. This adds a new migration; it does not edit old ones.
4. Run `/implement` and the tests again for the use cases that changed.

The specifications stay the source of truth; code follows them.
