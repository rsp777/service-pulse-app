import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Log Streaming Modal Component', () => {
  test.beforeEach(async ({ dashboardPage }) => {
    await dashboardPage.goto();
  });

  test('should display modal structure when activated and support closing', async ({ dashboardPage }) => {
    // Verify modal exists in DOM
    await expect(dashboardPage.logsModal.heading).toBeAttached();
    await expect(dashboardPage.logsModal.terminalBody).toBeAttached();
    await expect(dashboardPage.logsModal.closeButton).toBeAttached();

    // Trigger modal visibility
    await dashboardPage.page.evaluate(() => {
      const modal = document.getElementById('logs-modal');
      if (modal) modal.classList.add('active');
    });

    await expect(dashboardPage.logsModal.modal).toHaveClass(/active/);
    await expect(dashboardPage.logsModal.heading).toBeVisible();

    // Close modal via accessible close button
    await dashboardPage.logsModal.close();
    await expect(dashboardPage.logsModal.modal).not.toHaveClass(/active/);
  });

  test('should dismiss log streaming modal via ESC keyboard key', async ({ page, dashboardPage }) => {
    await dashboardPage.page.evaluate(() => {
      const modal = document.getElementById('logs-modal');
      if (modal) modal.classList.add('active');
    });

    await expect(dashboardPage.logsModal.modal).toHaveClass(/active/);
    await page.keyboard.press('Escape');
    await expect(dashboardPage.logsModal.modal).not.toHaveClass(/active/);
  });
});
