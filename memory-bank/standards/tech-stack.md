# Tech Stack

## Overview
Core technology decisions for this project. HRS is a hotel reservation system built as a single Java full-stack web app: Spring Boot with a server-side Vaadin Flow UI, run locally for the Spec-Driven Development workshop.

## Languages
Java 25

Set in `pom.xml` (`java.version`). Strong typing catches errors at compile time; modern features (records, sealed types, pattern matching) keep domain code concise.

## Framework
Spring Boot 4.1 + Vaadin Flow 25.3

Vaadin Flow builds the UI in server-side Java, so UI and business logic live in one codebase with no separate frontend or REST layer. Spring Boot provides dependency injection, configuration and the embedded server.

## Authentication
None for now

The app is open, with no login. Add Spring Security (with Vaadin's login view) later if requirements introduce roles or protected actions.

## Infrastructure & Deployment
Local only (workshop)

Run with `./mvnw spring-boot:run`, `HrsApplication.main()` from the IDE, or in GitHub Codespaces. There is no deployment target; revisit in the Operations phase if one is needed.

## Package Manager
Maven, through the `mvnw` wrapper

## Decision Relationships
- Vaadin Flow (server-side UI) means no API layer is needed between UI and services; views call Spring services directly.
- Running locally with no auth keeps the workshop setup to zero external dependencies, together with the embedded database (see `data-stack.md`).
