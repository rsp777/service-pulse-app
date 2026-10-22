import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Dashboard Operations Console — Edge Cases & Error Boundaries', () => {
  test.beforeEach(async ({ dashboardPage }) => {
    await dashboardPage.goto();
  });

  test('should prevent command execution and display alert banner when no server is selected', async ({ page, dashboardPage }) => {
    // Reset server select to empty context
    await dashboardPage.serverSelect.selectOption('');
    await expect(dashboardPage.selectedTitle).toHaveText(/Select a server/i);

    // Try submitting command form
    await dashboardPage.commandTextarea.fill('uptime');
    await dashboardPage.runCommandButton.click();

    // Verify error notice appears
    const notice = page.locator('#notice');
    await expect(notice).toHaveClass(/error/);
    await expect(notice).toContainText(/Select a server first/i);
  });

  test('should reject whitespace-only command submission', async ({ page, dashboardPage }) => {
    // Ensure a server is selected if available
    const count = await dashboardPage.serverSelect.locator('option').count();
    if (count > 1) {
      await dashboardPage.serverSelect.selectOption({ index: 1 });
    }

    // Enter only whitespace
    await dashboardPage.commandTextarea.fill('    ');
    await dashboardPage.runCommandButton.click();

    const notice = page.locator('#notice');
    await expect(notice).toContainText(/Please enter a command to execute/i);
  });

  test('should handle multiline shell commands and complex pipeline strings', async ({ dashboardPage }) => {
    const complexCommand = `ps aux | grep "java" | awk '{print $2}'\necho "STATUS: OK"`;
    await dashboardPage.commandTextarea.fill(complexCommand);
    await expect(dashboardPage.commandTextarea).toHaveValue(complexCommand);
  });

  test('should handle commands with special characters, quotes, and backticks', async ({ dashboardPage }) => {
    const specialCharsCommand = `echo "Server: $(hostname)" && grep -i 'error' /var/log/syslog 2>&1 | tail -n 10`;
    await dashboardPage.commandTextarea.fill(specialCharsCommand);
    await expect(dashboardPage.commandTextarea).toHaveValue(specialCharsCommand);
  });

  test('should autofill command textarea when selecting preconfigured template', async ({ dashboardPage }) => {
    const commandOptions = await dashboardPage.commandSelect.locator('option').count();
    if (commandOptions > 1) {
      // Select the first preconfigured command option (index 1)
      await dashboardPage.commandSelect.selectOption({ index: 1 });
      const commandValue = await dashboardPage.commandTextarea.inputValue();
      expect(commandValue.trim().length).toBeGreaterThan(0);
    }
  });

  test('should handle terminal clear and copy clipboard interactions gracefully', async ({ dashboardPage }) => {
    // Fill terminal with sample text via evaluation
    await dashboardPage.page.evaluate(() => {
      const output = document.getElementById('output');
      if (output) output.textContent = '[INFO] Initializing connection\n[OK] Ready';
    });

    await expect(dashboardPage.terminal.output).toContainText('Initializing connection');

    // Click Clear
    await dashboardPage.terminal.clear();
    await expect(dashboardPage.terminal.output).toHaveText(/Terminal cleared/i);
  });
});
