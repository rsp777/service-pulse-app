import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('System Logs Live SSE Viewer — Edge Cases & Buffer Controls', () => {
  test.beforeEach(async ({ systemLogsPage }) => {
    await systemLogsPage.goto();
    await systemLogsPage.sidebar.gotoSystemLogs();
  });

  test('should handle rapid toggling of pause and resume without breaking state', async ({ systemLogsPage }) => {
    // Toggle 4 times consecutively
    await systemLogsPage.toggleStream(); // paused
    await expect(systemLogsPage.pauseResumeButton).toHaveText(/Resume Stream/i);

    await systemLogsPage.toggleStream(); // resumed
    await expect(systemLogsPage.pauseResumeButton).toHaveText(/Pause Stream/i);

    await systemLogsPage.toggleStream(); // paused
    await expect(systemLogsPage.pauseResumeButton).toHaveText(/Resume Stream/i);

    await systemLogsPage.toggleStream(); // resumed
    await expect(systemLogsPage.pauseResumeButton).toHaveText(/Pause Stream/i);
  });

  test('should allow clearing an already empty log buffer multiple times without error', async ({ systemLogsPage }) => {
    await systemLogsPage.clearLogs();
    await expect(systemLogsPage.terminalBody).toHaveText(/No log entries matching criteria/i);

    // Second clear
    await systemLogsPage.clearLogs();
    await expect(systemLogsPage.terminalBody).toHaveText(/No log entries matching criteria/i);

    // Third clear
    await systemLogsPage.clearLogs();
    await expect(systemLogsPage.terminalBody).toHaveText(/No log entries matching criteria/i);
  });

  test('should cycle through all log severity levels without breaking filter state', async ({ systemLogsPage }) => {
    const levels = ['ALL', 'DEBUG', 'INFO', 'WARN', 'ERROR'] as const;
    for (const level of levels) {
      await systemLogsPage.filterByLevel(level);
      await expect(systemLogsPage.levelFilterSelect).toHaveValue(level);
    }
  });
});
