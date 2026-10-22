import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Preconfigured Command Templates Management', () => {
  test.beforeEach(async ({ commandsPage }) => {
    await commandsPage.goto();
    await commandsPage.sidebar.gotoCommands();
  });

  test('should display commands heading, toolbar controls, and command table', async ({ commandsPage }) => {
    await expect(commandsPage.heading).toBeVisible();
    await expect(commandsPage.refreshButton).toBeVisible();
    await expect(commandsPage.addCommandButton).toBeVisible();
    await expect(commandsPage.commandTable).toBeVisible();
  });

  test('should open and close Add Command modal with accessible elements', async ({ commandsPage }) => {
    await commandsPage.openAddCommandModal();
    await expect(commandsPage.commandModal.modal).toHaveClass(/active/);
    await expect(commandsPage.commandModal.heading).toBeVisible();

    // Verify inputs
    await expect(commandsPage.commandModal.nameInput).toBeVisible();
    await expect(commandsPage.commandModal.parametersInput).toBeVisible();
    await expect(commandsPage.commandModal.descriptionInput).toBeVisible();

    // Fill form
    await commandsPage.commandModal.fill({
      name: 'Check Disk Space',
      parameters: 'df -h',
      description: 'Checks disk partition usage across mounts'
    });

    await expect(commandsPage.commandModal.nameInput).toHaveValue('Check Disk Space');
    await expect(commandsPage.commandModal.parametersInput).toHaveValue('df -h');

    // Cancel modal
    await commandsPage.commandModal.cancel();
    await expect(commandsPage.commandModal.modal).not.toHaveClass(/active/);
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
