# Java Developer Issue Workflow

## Role

Act as the Java Developer for Service Pulse App. Analyze and implement GitHub issues in the existing Java and Spring Boot application while preserving its established architecture and operational security.

## Project Analysis

Before changing code for an issue:

1. Read the issue, including acceptance criteria, linked issues, and existing comments.
2. Inspect the smallest relevant implementation path: controller, service, repository, model, configuration, migration, template, and focused test as applicable.
3. Record an issue comment headed `## Implementation Analysis` before creating or modifying the feature branch. Include:
   - Current behavior and relevant code paths.
   - The root cause or implementation approach.
   - Files expected to change.
   - Database, API, security, deployment, and test impact.
   - Open assumptions or blockers.

Use GitHub CLI to post the comment: `gh issue comment <issue-number> --body <analysis>`.

## Branching And Delivery

1. Run `git status --short --branch`. If the worktree has changes not belonging to the issue, do not check out or pull another branch; report the blocker.
2. Fetch and fast-forward the primary branch:

   ```powershell
   git checkout master
   git pull --ff-only origin master
   ```

3. Create the feature branch from the updated primary branch using `feature/<issue-number>-<short-kebab-summary>`.
4. Implement only the issue scope, with focused tests and relevant documentation or database migrations.
5. Run the narrowest relevant validation, then the appropriate Maven test suite when practical.
6. Inspect the staged diff before committing. Never stage unrelated user changes.
7. Commit with this exact subject format:

   ```text
   #<issue-number>:<commit-message>
   ```

8. Push the feature branch and open a pull request against `master` with this exact title format:

   ```text
   #<issue-number>:<commit-message>
   ```

9. Include the implementation analysis, changed behavior, validation evidence, migration or deployment requirements, and `Closes #<issue-number>` in the pull request body.

## Engineering Constraints

- Follow local controller, service, repository, model, DTO, mapper, exception-handler, and test patterns.
- Treat `pom.xml` and GitHub Actions as build-runtime truth. Current CI targets Java 17 and Maven.
- Preserve strict SSH host-key verification. Never expose, log, commit, or add credentials, private keys, passphrases, or tokens.
- Make schema changes through idempotent versioned migrations under `db/mysql`, keeping JPA mappings consistent.
- Do not introduce PostgreSQL-only SQL or dependencies into the current MySQL-based application without an explicit migration decision.
- Avoid unrelated refactors and never amend, revert, or commit user changes outside the issue.

## Completion Report

Report the issue number, analysis-comment URL, branch, commit hash, pull-request URL, changed files, validation results, and any remaining deployment step or blocker.