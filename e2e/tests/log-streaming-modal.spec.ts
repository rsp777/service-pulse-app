import { test, expect } from '../src/fixtures/test-fixtures';

/**
 * E2E tests for Issue #31: Live Service Log Stream Modal Missing Controls & Top Line Clipping.
 * Author: Ravindra Singh Pawar / Antigravity
 */
test.describe('Issue #31: Live Service Log Streaming Modal Controls & Verification', () => {
  test.beforeEach(async ({ dashboardPage }) => {
    await dashboardPage.goto();
  });

  // Issue #31: Positive test - Modal structure, controls presence, and padding-top >= 12px
  test('should display all stream controls and assert padding-top is 12px preventing clipping', async ({ dashboardPage }) => {
    const modal = dashboardPage.logsModal;

    // Open modal
    await dashboardPage.page.evaluate(() => {
      const el = document.getElementById('logs-modal');
      if (el) el.classList.add('active');
    });

    await expect(modal.modal).toBeVisible();
    await expect(modal.heading).toBeVisible();
    await expect(modal.filterTextInput).toBeVisible();
    await expect(modal.levelFilterSelect).toBeVisible();
    await expect(modal.autoScrollCheckbox).toBeVisible();
    await expect(modal.pauseButton).toBeVisible();
    await expect(modal.copyButton).toBeVisible();
    await expect(modal.clearButton).toBeVisible();
    await expect(modal.terminalBody).toBeVisible();
    await expect(modal.footerCloseButton).toBeVisible();

    // Verify padding-top: 12px on logs-output container
    const paddingTop = await modal.terminalBody.evaluate((el) => {
      return window.getComputedStyle(el).paddingTop;
    });
    expect(paddingTop).toBe('12px');

    await modal.close();
    await expect(modal.modal).not.toHaveClass(/active/);
  });

  // Issue #31: Positive & Negative test - Filter logs by log level dropdown
  test('should filter service logs by level dropdown (ALL, INFO, WARN, ERROR)', async ({ dashboardPage }) => {
    const modal = dashboardPage.logsModal;

    // Open modal and inject sample log lines into buffer
    await dashboardPage.page.evaluate(() => {
      const el = document.getElementById('logs-modal');
      if (el) el.classList.add('active');
      window.serviceLogBuffer = [
        '[2026-10-22 12:00:01] [INFO] Service application booted successfully',
        '[2026-10-22 12:00:02] [WARN] Connection pool saturation at 85%',
        '[2026-10-22 12:00:03] [ERROR] NullPointerException in TransactionManager'
      ];
      if (typeof window.renderServiceLogs === 'function') {
        window.renderServiceLogs();
      }
    });

    // Default 'ALL': all 3 lines present
    let content = await modal.getLogText();
    expect(content).toContain('[INFO]');
    expect(content).toContain('[WARN]');
    expect(content).toContain('[ERROR]');

    // Filter to ERROR: only ERROR present (+ve), INFO and WARN hidden (-ve)
    await modal.levelFilterSelect.selectOption('ERROR');
    content = await modal.getLogText();
    expect(content).toContain('[ERROR] NullPointerException');
    expect(content).not.toContain('[INFO]');
    expect(content).not.toContain('[WARN]');

    // Filter to WARN: only WARN present
    await modal.levelFilterSelect.selectOption('WARN');
    content = await modal.getLogText();
    expect(content).toContain('[WARN] Connection pool saturation');
    expect(content).not.toContain('[INFO]');
    expect(content).not.toContain('[ERROR]');

    // Restore to ALL
    await modal.levelFilterSelect.selectOption('ALL');
    content = await modal.getLogText();
    expect(content).toContain('[INFO]');
    expect(content).toContain('[WARN]');
    expect(content).toContain('[ERROR]');

    await modal.close();
  });

  // Issue #31: Positive & Negative test - Text query filtering
  test('should filter logs by text query and handle empty results', async ({ dashboardPage }) => {
    const modal = dashboardPage.logsModal;

    await dashboardPage.page.evaluate(() => {
      const el = document.getElementById('logs-modal');
      if (el) el.classList.add('active');
      window.serviceLogBuffer = [
        '[2026-10-22 12:00:01] [INFO] DispatcherServlet initialized',
        '[2026-10-22 12:00:02] [INFO] Tomcat started on port 8080',
        '[2026-10-22 12:00:03] [ERROR] Database timeout occurred'
      ];
      if (typeof window.renderServiceLogs === 'function') {
        window.renderServiceLogs();
      }
    });

    // Positive filter: matching "Tomcat"
    await modal.filterTextInput.fill('Tomcat');
    let content = await modal.getLogText();
    expect(content).toContain('Tomcat started on port 8080');
    expect(content).not.toContain('DispatcherServlet');
    expect(content).not.toContain('Database timeout');

    // Negative filter: no matches
    await modal.filterTextInput.fill('NonExistentKeywordXYZ');
    content = await modal.getLogText();
    expect(content).toContain('No log entries match criteria');

    // Clear filter
    await modal.filterTextInput.fill('');
    content = await modal.getLogText();
    expect(content).toContain('DispatcherServlet initialized');
    expect(content).toContain('Tomcat started on port 8080');

    await modal.close();
  });

  // Issue #31: Positive test - Pause and Resume stream controls
  test('should toggle stream pause state and update button label and status badge', async ({ dashboardPage }) => {
    const modal = dashboardPage.logsModal;

    await dashboardPage.page.evaluate(() => {
      const el = document.getElementById('logs-modal');
      if (el) el.classList.add('active');
    });

    await expect(modal.pauseButton).toHaveText('Pause Stream');

    // Click Pause
    await modal.pauseButton.click();
    await expect(modal.pauseButton).toHaveText('Resume Stream');
    await expect(modal.streamStatus).toContainText('Paused');

    // Click Resume
    await modal.pauseButton.click();
    await expect(modal.pauseButton).toHaveText('Pause Stream');
    await expect(modal.streamStatus).toContainText('Streaming');

    await modal.close();
  });

  // Issue #31: Positive test - Clear button empties terminal buffer
  test('should clear terminal content when Clear button is clicked', async ({ dashboardPage }) => {
    const modal = dashboardPage.logsModal;

    await dashboardPage.page.evaluate(() => {
      const el = document.getElementById('logs-modal');
      if (el) el.classList.add('active');
      window.serviceLogBuffer = ['Line 1', 'Line 2', 'Line 3'];
      if (typeof window.renderServiceLogs === 'function') {
        window.renderServiceLogs();
      }
    });

    let content = await modal.getLogText();
    expect(content).toContain('Line 1');

    await modal.clearButton.click();
    content = await modal.getLogText();
    expect(content.trim()).toBe('');
    await expect(modal.lineCount).toHaveText('(0 lines)');

    await modal.close();
  });

  // Issue #31: Positive test - Copy button interaction
  test('should trigger copy action and show visual confirmation', async ({ dashboardPage }) => {
    const modal = dashboardPage.logsModal;

    await dashboardPage.page.evaluate(() => {
      const el = document.getElementById('logs-modal');
      if (el) el.classList.add('active');
      const terminal = document.getElementById('logs-output');
      if (terminal) terminal.textContent = 'Log content for clipboard test';
    });

    await modal.copyButton.click();
    await expect(modal.copyButton).toHaveText('Copied!');

    await modal.close();
  });

  // Issue #31: Stream cleanup & modal closing via footer, 'x', and Escape key
  test('should dismiss modal and close active EventSource stream cleanly', async ({ page, dashboardPage }) => {
    const modal = dashboardPage.logsModal;

    // 1. Dismiss via Close Stream button in footer
    await dashboardPage.page.evaluate(() => {
      const el = document.getElementById('logs-modal');
      if (el) el.classList.add('active');
      window.logStreamSource = { close: () => { window._testStreamClosed = true; } };
    });
    await expect(modal.modal).toHaveClass(/active/);
    await modal.closeViaFooter();
    await expect(modal.modal).not.toHaveClass(/active/);

    const wasClosedViaFooter = await page.evaluate(() => window._testStreamClosed);
    expect(wasClosedViaFooter).toBe(true);

    // 2. Dismiss via close 'x' button
    await dashboardPage.page.evaluate(() => {
      const el = document.getElementById('logs-modal');
      if (el) el.classList.add('active');
      window._testStreamClosed2 = false;
      window.logStreamSource = { close: () => { window._testStreamClosed2 = true; } };
    });
    await expect(modal.modal).toHaveClass(/active/);
    await modal.close();
    await expect(modal.modal).not.toHaveClass(/active/);

    const wasClosedViaX = await page.evaluate(() => window._testStreamClosed2);
    expect(wasClosedViaX).toBe(true);

    // 3. Dismiss via Escape key
    await dashboardPage.page.evaluate(() => {
      const el = document.getElementById('logs-modal');
      if (el) el.classList.add('active');
      window._testStreamClosed3 = false;
      window.logStreamSource = { close: () => { window._testStreamClosed3 = true; } };
    });
    await expect(modal.modal).toHaveClass(/active/);
    await page.keyboard.press('Escape');
    await expect(modal.modal).not.toHaveClass(/active/);

    const wasClosedViaEscape = await page.evaluate(() => window._testStreamClosed3);
    expect(wasClosedViaEscape).toBe(true);
  });
});
