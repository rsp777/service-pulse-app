# Agent Rule: Issue Implementation, Testing & Git Lifecycle Workflow

All Antigravity agents working in this workspace must strictly follow this lifecycle for every GitHub issue:

## 1. Branch Isolation
- For every issue, checkout a dedicated feature or bugfix branch branched off `dev`:
  - `fix/issue-<number>-<short-description>` for bugs/gaps.
  - `feature/issue-<number>-<short-description>` for new capabilities/enhancements.
- Never commit directly to `dev` or `main`.

## 2. Code Traceability & Annotations
- Whenever writing or modifying code in Java, HTML, CSS, or JS, add explicit issue tracking comments with author attribution:
  - Java:
    ```java
    // Issue #<number>: <feature/fix description> [Author: Ravindra Singh Pawar / Antigravity]
    ```
  - JavaScript / CSS:
    ```javascript
    // Issue #<number>: <feature/fix description> [Author: Ravindra Singh Pawar / Antigravity]
    ```
  - HTML:
    ```html
    <!-- Issue #<number>: <feature/fix description> [Author: Ravindra Singh Pawar / Antigravity] -->
    ```

## 3. Dual-Layer Verification (Manual + Automated)
- As soon as the implementation is in place:
  1. **Manual Testing:** Perform live verification against running backend/frontend with real data.
  2. **Automation Testing:** Write and execute automated unit tests (JUnit 5) and/or Playwright E2E test specs in `e2e/tests/`.
- Provide clear, reproducible certification steps in the response/commit notes.

## 4. Comprehensive Test Scenarios (+ve and -ve)
- Every verification report and test suite must explicitly document and execute:
  - **Positive (+ve) Test Cases:** Valid input handling, happy paths, proper state updates, and expected UI rendering.
  - **Negative (-ve) Test Cases:** Invalid inputs, blank fields, edge cases, error fallbacks, and fail-closed behaviors.

## 5. Defect Management & Sub-Issue Creation
- If any test scenario fails during testing:
  - Do NOT merge to `dev`.
  - Create a sub-issue on GitHub labeled as `bug`, linking to the parent issue.
  - Address and resolve the defect before proceeding.

## 6. Merge to `dev` and Branch Cleanup
- Only after both manual testing and automation testing have completely succeeded:
  1. Merge the branch into `dev`.
  2. Delete the local and remote source branch.
  3. Update the GitHub issue with full certification details and close it.
