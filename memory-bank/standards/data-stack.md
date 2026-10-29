# Data Stack

## Overview
Database and data persistence decisions for this project. HRS uses an embedded H2 database, a schema managed by Flyway migrations, and type-safe SQL through jOOQ.

## Database
H2 (embedded)

Runs inside the application with nothing to install, which fits a local workshop app. The H2 console (`spring-boot-h2console`) is available for inspecting data during development.

## ORM / Database Client
jOOQ, with Flyway for migrations

- **Flyway** owns the schema: every change is a new versioned script in `src/main/resources/db/migration/V{n}__{description}.sql`. Applied migrations are never edited.
- **jOOQ** generates type-safe Java classes from the migrated schema during `generate-sources` (`./mvnw compile`). All data access uses jOOQ's DSL; no JPA/Hibernate.

## Decision Relationships
- The database schema is the single source of truth. Change order: write a Flyway migration → regenerate jOOQ code → update the code that uses it.
- jOOQ fits Vaadin Flow well: views call Spring services that run jOOQ queries directly, with no entity mapping layer in between.
