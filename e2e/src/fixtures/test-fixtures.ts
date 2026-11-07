import { test as base } from '@playwright/test';
import { DashboardPage } from '../pages/DashboardPage';
import { ServersPage } from '../pages/ServersPage';
import { ServicesPage } from '../pages/ServicesPage';
import { CommandsPage } from '../pages/CommandsPage';
import { AlertsPage } from '../pages/AlertsPage';
import { SystemLogsPage } from '../pages/SystemLogsPage';
import { SettingsPage } from '../pages/SettingsPage';

/**
 * Custom Playwright test fixture injecting pre-instantiated page objects.
 */
type PageFixtures = {
  dashboardPage: DashboardPage;
  serversPage: ServersPage;
  servicesPage: ServicesPage;
  commandsPage: CommandsPage;
  alertsPage: AlertsPage;
  systemLogsPage: SystemLogsPage;
  settingsPage: SettingsPage;
};

export const test = base.extend<PageFixtures>({
  dashboardPage: async ({ page }, use) => {
    const dashboard = new DashboardPage(page);
    await use(dashboard);
  },
  serversPage: async ({ page }, use) => {
    const servers = new ServersPage(page);
    await use(servers);
  },
  servicesPage: async ({ page }, use) => {
    const services = new ServicesPage(page);
    await use(services);
  },
  commandsPage: async ({ page }, use) => {
    const commands = new CommandsPage(page);
    await use(commands);
  },
  alertsPage: async ({ page }, use) => {
    const alerts = new AlertsPage(page);
    await use(alerts);
  },
  systemLogsPage: async ({ page }, use) => {
    const logs = new SystemLogsPage(page);
    await use(logs);
  },
  settingsPage: async ({ page }, use) => {
    const settings = new SettingsPage(page);
    await use(settings);
  },
});

export { expect } from '@playwright/test';
