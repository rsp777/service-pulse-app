# Service Pulse — Playwright TypeScript E2E UI Automation Suite

A modern, robust End-to-End (E2E) UI test automation framework built with **Playwright**, **TypeScript**, **Page Object Model (POM)**, and **Component Object Model (COM)**, utilizing a strict **Role-Based Locator-First Strategy**.

---

## Architecture Overview

```
e2e/
├── package.json               # NPM scripts and dependencies
├── tsconfig.json              # TypeScript compilation config with path aliases
├── playwright.config.ts       # Multi-browser, trace, reporter, and base URL config
├── src/
│   ├── components/            # Reusable UI component abstractions
│   │   ├── BaseComponent.ts
│   │   ├── SidebarNavComponent.ts
│   │   ├── HeaderComponent.ts
│   │   ├── StatsOverviewComponent.ts
│   │   ├── ServerModalComponent.ts
│   │   ├── ServiceModalComponent.ts
│   │   ├── LogsModalComponent.ts
│   │   └── TerminalComponent.ts
│   ├── pages/                 # Page Object Model (POM) representations
│   │   ├── BasePage.ts
│   │   ├── DashboardPage.ts
│   │   ├── ServersPage.ts
│   │   ├── ServicesPage.ts
│   │   ├── CommandsPage.ts
│   │   ├── AlertsPage.ts
│   │   └── SystemLogsPage.ts
│   └── fixtures/              # Custom test fixtures with auto-injected page objects
│       └── test-fixtures.ts
└── tests/                     # Test suites using role-based locators
    ├── navigation.spec.ts
    ├── dashboard.spec.ts
    ├── server-management.spec.ts
    ├── service-management.spec.ts
    ├── log-streaming-modal.spec.ts
    └── system-logs.spec.ts
```

---

## Role-Based Locator Strategy

This framework strictly enforces Playwright's **User-Facing / Accessible Role Locator Strategy**:
1. `page.getByRole('button', { name: '...' })`
2. `page.getByRole('heading', { name: '...', level: 2 })`
3. `page.getByRole('navigation')`
4. `page.getByRole('status')`
5. `page.getByLabel('...')`
6. `page.getByPlaceholder('...')`

---

## Installation & Setup

### Option 1: Standalone Workspace (Separate Window)
Open the `e2e` directory in a new terminal or IDE window:
```bash
cd c:\Users\Ravi\spring_practice\service-pulse-app\e2e

# 1. One-step install (NPM dependencies + Playwright browser binaries)
npm run setup

# OR step-by-step:
npm install
npx playwright install --with-deps chromium
```

### Option 2: Install via Maven (from Project Root)
From the root of `service-pulse-app`:
```bash
# Triggers 'npm install' and 'npx playwright install --with-deps chromium' via Maven
mvn test -Pe2e-install
```

---

## How to Run the Tests

### From the `e2e` Directory:
```bash
# Run all UI tests (headless)
npm test

# Run with interactive Playwright UI mode
npm run test:ui

# Run in headed mode (watch browser open)
npm run test:headed

# Run a specific spec
npx playwright test tests/navigation.spec.ts

# View HTML Test Report
npm run report
```

### From the Root Project via Maven:
```bash
# Trigger Playwright TypeScript suite via Maven
mvn test -Pe2e
```
*(Optionally pass a custom base URL: `mvn test -Pe2e -DBASE_URL=http://localhost:9092/service-pulse-app/`)*.
