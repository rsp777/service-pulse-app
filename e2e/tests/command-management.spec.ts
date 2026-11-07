// Issue #29: Preconfigured Commands Execution & Target Server Context Tests [Author: Ravindra Singh Pawar / Antigravity]
import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Preconfigured Command Templates Management', () => {
  test.beforeEach(async ({ commandsPage }) => {
    await commandsPage.goto();
    await commandsPage.sidebar.gotoCommands();
  });

  // Issue #29: Verify toolbar controls include target server selector and updated table columns
  test('should display commands heading, target server selector, and refactored table columns', async ({ commandsPage }) => {
    await expect(commandsPage.heading).toBeVisible();
    await expect(commandsPage.targetServerSelect).toBeVisible();
    await expect(commandsPage.refreshButton).toBeVisible();
    await expect(commandsPage.addCommandButton).toBeVisible();
    await expect(commandsPage.commandTable).toBeVisible();

    const headers = commandsPage.commandTable.locator('th');
    await expect(headers.getByText(/Template Name/i)).toBeVisible();
    await expect(headers.getByText(/Command String/i)).toBeVisible();
    await expect(headers.getByText(/Last Exit Code/i)).toBeVisible();
    await expect(headers.getByText(/Last Executed At/i)).toBeVisible();
  });

  // Issue #29: Verify refactored modal labels for Template Name and Command String
  test('should open and close Add Command modal with refactored fields', async ({ page, commandsPage }) => {
    await commandsPage.openAddCommandModal();
    await expect(commandsPage.commandModal.modal).toHaveClass(/active/);
    await expect(commandsPage.commandModal.heading).toBeVisible();

    // Verify inputs & labels
    await expect(page.getByText('Template Name *')).toBeVisible();
    await expect(page.getByText('Command String *')).toBeVisible();
    await expect(commandsPage.commandModal.nameInput).toBeVisible();
    await expect(commandsPage.commandModal.parametersInput).toBeVisible();
    await expect(commandsPage.commandModal.descriptionInput).toBeVisible();

    // Fill form
    await commandsPage.commandModal.fill({
      name: 'Check Memory Usage',
      parameters: 'free -m',
      description: 'Checks memory usage across buffers and cache'
    });

    await expect(commandsPage.commandModal.nameInput).toHaveValue('Check Memory Usage');
    await expect(commandsPage.commandModal.parametersInput).toHaveValue('free -m');

    // Cancel modal
    await commandsPage.commandModal.cancel();
    await expect(commandsPage.commandModal.modal).not.toHaveClass(/active/);
  });

  // Issue #29: Negative test - Warning when target server is not selected [Author: Ravindra Singh Pawar / Antigravity]
  test('should warn user if target server is not selected before running command', async ({ page, commandsPage }) => {
    const runButtons = commandsPage.commandTable.locator('[data-cmd-run]');
    const count = await runButtons.count();
    if (count > 0) {
      // Unselect target server by selecting blank option
      await commandsPage.targetServerSelect.selectOption('');
      await runButtons.first().click();

      const notice = page.locator('#notice');
      await expect(notice).toBeVisible();
      await expect(notice).toContainText(/select a target server/i);
      await expect(page).toHaveURL(/#commands/);
    }
  });

  // Issue #29: Positive test - Execute preconfigured command against target server [Author: Ravindra Singh Pawar / Antigravity]
  test('should execute preconfigured command against target server context', async ({ page, commandsPage }) => {
    const runButtons = commandsPage.commandTable.locator('[data-cmd-run]');
    const count = await runButtons.count();
    if (count > 0) {
      // Select first available server option
      await commandsPage.targetServerSelect.selectOption({ index: 1 });
      await runButtons.first().click();

      // Should navigate to dashboard and output terminal
      await expect(page).toHaveURL(/#dashboard/);
      const terminal = page.locator('#output');
      await expect(terminal).toBeVisible();
      await expect(terminal).toContainText(/Executing:/i);
    }
  });

  test('should dismiss Add Command modal via close (x) button', async ({ commandsPage }) => {
    await commandsPage.openAddCommandModal();
    await expect(commandsPage.commandModal.modal).toHaveClass(/active/);

    await commandsPage.commandModal.close();
    await expect(commandsPage.commandModal.modal).not.toHaveClass(/active/);
  });

  test('should dismiss Add Command modal via ESC key', async ({ page, commandsPage }) => {
    await commandsPage.openAddCommandModal();
    await expect(commandsPage.commandModal.modal).toHaveClass(/active/);

    await page.keyboard.press('Escape');
    await expect(commandsPage.commandModal.modal).not.toHaveClass(/active/);
  });

  test('should enforce required HTML5 attributes on command name and parameters', async ({ commandsPage }) => {
    await commandsPage.openAddCommandModal();
    await expect(commandsPage.commandModal.nameInput).toHaveAttribute('required', '');
    await expect(commandsPage.commandModal.parametersInput).toHaveAttribute('required', '');
    await commandsPage.commandModal.cancel();
  });
});
