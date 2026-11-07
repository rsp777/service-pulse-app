import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Navigation & Workspace Routing', () => {
  test.beforeEach(async ({ dashboardPage }) => {
    await dashboardPage.goto();
  });

  test('should display brand title and default active Dashboard page', async ({ page, dashboardPage }) => {
    await expect(dashboardPage.sidebar.brandTitle).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Infrastructure & Operations', level: 2 })).toBeVisible();
    await expect(dashboardPage.sidebar.dashboardButton).toHaveClass(/active/);
  });

  test('should navigate between views using role-based sidebar buttons', async ({ page, dashboardPage }) => {
    // Navigate to Servers
    await dashboardPage.sidebar.gotoServers();
    await expect(page.getByRole('heading', { name: 'Server Inventory', level: 2 })).toBeVisible();
    await expect(dashboardPage.sidebar.serversButton).toHaveClass(/active/);

    // Navigate to Services
    await dashboardPage.sidebar.gotoServices();
    await expect(page.getByRole('heading', { name: 'Service Registry', level: 2 })).toBeVisible();
    await expect(dashboardPage.sidebar.servicesButton).toHaveClass(/active/);

    // Navigate to Commands
    await dashboardPage.sidebar.gotoCommands();
    await expect(page.getByRole('heading', { name: 'Preconfigured Commands', level: 2 })).toBeVisible();
    await expect(dashboardPage.sidebar.commandsButton).toHaveClass(/active/);

    // Navigate to Alerts
    await dashboardPage.sidebar.gotoAlerts();
    await expect(page.getByRole('heading', { name: /Alert & Condition Management/i, level: 2 })).toBeVisible();
    await expect(dashboardPage.sidebar.alertsButton).toHaveClass(/active/);

    // Navigate to System Logs
    await dashboardPage.sidebar.gotoSystemLogs();
    await expect(page.getByRole('heading', { name: /Live System Logs/i, level: 2 })).toBeVisible();
    await expect(dashboardPage.sidebar.logsButton).toHaveClass(/active/);

    // Navigate to Settings
    await dashboardPage.sidebar.gotoSettings();
    await expect(page.getByRole('heading', { name: 'Runtime Configuration', level: 2 })).toBeVisible();
    await expect(dashboardPage.sidebar.settingsButton).toHaveClass(/active/);

    // Navigate back to Dashboard
    await dashboardPage.sidebar.gotoDashboard();
    await expect(dashboardPage.sidebar.dashboardButton).toHaveClass(/active/);
  });

  test('should toggle notification dropdown from header', async ({ dashboardPage }) => {
    await expect(dashboardPage.header.notificationDropdown).not.toHaveClass(/active/);
    await dashboardPage.header.toggleNotifications();
    await expect(dashboardPage.header.notificationDropdown).toHaveClass(/active/);
    await expect(dashboardPage.header.markAllReadButton).toBeVisible();
  });
});
