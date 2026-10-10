# Project Issue Tracker

This document tracks technical blockers, bugs, and lessons learned during the development of the Service Monitoring Tool.

## Story 1: Project Structure & Config Management
- **Issue**: None.

## Story 2: Java Probe Engine - Core Infrastructure
- **Issue**: Dependency Resolution Failure.
- **Symptom**: `Could not resolve dependencies for project com.example:collector:jar:1.0-SNAPSHOT` for `micronaut-data-jdbc`.
- **Root Cause**: Version mismatch or missing artifacts in the central repository for specific Micronaut Data versions.
- **Resolution**: Simplified the architecture to use **Pure JDBC + HikariCP**. This removed the problematic Micronaut Data abstractions while maintaining high performance and lightness.

## Story 3: Network Probes
- **Issue**: Java Compilation Error (File Naming).
- **Symptom**: `interface Probe is public, should be declared in a file named Probe.java`.
- **Root Cause**: The `Probe` interface was declared as `public` inside `Probes.java`.
- **Resolution**: Renamed `Probes.java` to `Probe.java`.

## Story 4: SSH System Probe
- **Issue**: None.

## Story 5: Dashboard API Layer
- **Issue**: Environment Variable Parsing Error.
- **Symptom**: `getaddrinfo ENOTFOUND ubuntu-dell\nDB_PORT=5432...`.
- **Root Cause**: `.env.local` was written with literal `\n` characters or improper line breaks, causing the `pg` pool to read the entire file as a single hostname.
- **Resolution**: Rewrote the `.env.local` file using a clean shell redirection to ensure proper POSIX line endings.

## Story 6: Dashboard UI
- **Issue**: TypeScript Compilation Errors.
- **Symptom**: `Parameter 'service' implicitly has an 'any' type` and `Could not find a declaration file for module 'pg'`.
- **Root Cause**: Missing type definitions for the `pg` library and implicit `any` types in `.map()` functions.
- **Resolution**: Installed `@types/pg` and added explicit `: any` typing to the service mapping function.

## Story 7: Containerization & Sanity Testing
- **Issue 1**: Java JAR Execution Failure (Missing Manifest).
- **Symptom**: `no main manifest attribute, in .\\collector-1.0-SNAPSHOT.jar`.
- **Root Cause**: The `pom.xml` lacked the `maven-jar-plugin` configuration to specify the `Main-Class` in the MANIFEST.MF file.
- **Resolution**: Added `maven-jar-plugin` to `pom.xml` to define `com.example.collector.Application` as the entry point.
- **Status**: Completed.

- **Issue 2**: Java JAR Execution Failure (ClassNotFoundException).
- **Symptom**: `Error: Unable to initialize main class com.example.collector.Application` / `java.lang.NoClassDefFoundError: io/micronaut/runtime/server/event/ServerStartupEvent`.
- **Root Cause**: The standard JAR only contains the project's compiled classes, not its dependencies (the Micronaut runtime).
- **Resolution**: Implemented the `maven-shade-plugin` to create an **Uber-JAR** (fat JAR). This packages all required dependencies into a single executable file.
- **Status**: In Progress (Build Successful).
