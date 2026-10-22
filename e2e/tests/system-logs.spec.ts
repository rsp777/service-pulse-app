import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('System Logs Live Viewer', () => {
  test.beforeEach(async ({ systemLogsPage }) => {
    await systemLogsPage.goto();
    await systemLogsPage.sidebar.gotoSystemLogs();
  });

  test('should display System Logs heading, stream toggle, and log viewer', async ({ systemLogsPage }) => {
    await expect(systemLogsPage.heading).toBeVisible();
    await expect(systemLogsPage.pauseResumeButton).toBeVisible();
    await expect(systemLogsPage.clearLogsButton).toBeVisible();
    await expect(systemLogsPage.levelFilterSelect).toBeVisible();
    await expect(systemLogsPage.terminalBody).toBeVisible();
  });

  test('should toggle stream pause and resume state', async ({ systemLogsPage }) => {
    // Initial button text is "Pause Stream"
    await expect(systemLogsPage.pauseResumeButton).toHaveText(/Pause Stream/i);

    // Click to pause
    await systemLogsPage.toggleStream();
    await expect(systemLogsPage.pauseResumeButton).toHaveText(/Resume Stream/i);

    // Click to resume
    await systemLogsPage.toggleStream();
    await expect(systemLogsPage.pauseResumeButton).toHaveText(/Pause Stream/i);
  });

  test('should clear logs console when Clear is clicked', async ({ systemLogsPage }) => {
    await systemLogsPage.clearLogs();
    await expect(systemLogsPage.terminalBody).toHaveText(/No log entries matching criteria/i);
  });

  test('should allow selecting log level filter options', async ({ systemLogsPage }) => {
    await expect(systemLogsPage.levelFilterSelect).toBeVisible();
    await systemLogsPage.filterByLevel('ERROR');
    await expect(systemLogsPage.levelFilterSelect).toHaveValue('ERROR');

    await systemLogsPage.filterByLevel('INFO');
    await expect(systemLogsPage.levelFilterSelect).toHaveValue('INFO');

    await systemLogsPage.filterByLevel('ALL');
    await expect(systemLogsPage.levelFilterSelect).toHaveValue('ALL');
  });

  test('should toggle auto-scroll checkbox', async ({ page }) => {
    const autoScrollCheckbox = page.locator('#log-autoscroll-toggle');
    await expect(autoScrollCheckbox).toBeVisible();
    await expect(autoScrollCheckbox).toBeChecked();

    await autoScrollCheckbox.uncheck();
    await expect(autoScrollCheckbox).not.toBeChecked();

    await autoScrollCheckbox.check();
    await expect(autoScrollCheckbox).toBeChecked();
  });
});
