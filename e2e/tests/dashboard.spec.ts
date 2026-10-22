import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Dashboard Operations Console', () => {
  test.beforeEach(async ({ dashboardPage }) => {
    await dashboardPage.goto();
  });

  test('should display all 4 operational stat cards with metrics', async ({ dashboardPage }) => {
    await expect(dashboardPage.stats.registeredServersCount).toBeVisible();
    await expect(dashboardPage.stats.servicesOnlineCount).toBeVisible();
    await expect(dashboardPage.stats.activeAlertsCount).toBeVisible();
    await expect(dashboardPage.stats.sshStatus).toBeVisible();
  });

  test('should show empty server selection state when reset', async ({ dashboardPage }) => {
    await dashboardPage.serverSelect.selectOption('');
    await expect(dashboardPage.selectedTitle).toHaveText(/Select a server/i);
    await expect(dashboardPage.serviceList).toContainText(/Select a server to load services/i);
  });

  test('should provide role-based interactive terminal controls', async ({ dashboardPage }) => {
    await expect(dashboardPage.terminal.output).toBeVisible();
    await expect(dashboardPage.terminal.clearButton).toBeEnabled();
    await expect(dashboardPage.terminal.copyButton).toBeEnabled();

    // Verify clear updates terminal output
    await dashboardPage.terminal.clear();
    await expect(dashboardPage.terminal.output).toHaveText(/Terminal cleared/i);
  });

  test('should require a command before submitting the command form', async ({ dashboardPage }) => {
    await expect(dashboardPage.runCommandButton).toBeVisible();
    await expect(dashboardPage.commandTextarea).toHaveAttribute('required', '');
  });

  test('should update selected server title and metadata upon server selection', async ({ dashboardPage }) => {
    // Check if options exist
    const optionsCount = await dashboardPage.serverSelect.locator('option').count();
    if (optionsCount > 1) {
      await dashboardPage.serverSelect.selectOption({ index: 1 });
      const selectedValue = await dashboardPage.serverSelect.inputValue();
      expect(selectedValue).toBeTruthy();
      await expect(dashboardPage.selectedTitle).not.toHaveText(/Select a server/i);
    }
  });

  test('should verify monitored services card toolbar controls', async ({ dashboardPage }) => {
    await expect(dashboardPage.refreshServicesButton).toBeVisible();
    await expect(dashboardPage.addServiceButton).toBeVisible();
  });
});
