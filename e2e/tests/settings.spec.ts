import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Runtime Configuration & Settings Management', () => {
  test.beforeEach(async ({ settingsPage }) => {
    await settingsPage.goto();
    await settingsPage.sidebar.gotoSettings();
  });

  test('should display Runtime Configuration view and controls', async ({ settingsPage }) => {
    await expect(settingsPage.heading).toBeVisible();
    await expect(settingsPage.healthcheckCheckbox).toBeVisible();
    await expect(settingsPage.serviceManagementCheckbox).toBeVisible();
    await expect(settingsPage.alertManagementCheckbox).toBeVisible();
    await expect(settingsPage.healthStatusPopulationCheckbox).toBeVisible();
    await expect(settingsPage.healthcheckCronInput).toBeVisible();
    await expect(settingsPage.saveConfigButton).toBeVisible();
  });

  test('should verify healthcheck cron schedule input is readonly', async ({ settingsPage }) => {
    await expect(settingsPage.healthcheckCronInput).toHaveAttribute('readonly', '');
  });

  test('should toggle runtime configuration checkboxes', async ({ settingsPage }) => {
    await expect(settingsPage.runtimeConfigStatus).toHaveText(/Configuration Loaded|Unavailable/);
    const wasChecked = await settingsPage.healthcheckCheckbox.isChecked();
    await settingsPage.healthcheckCheckbox.setChecked(!wasChecked);
    await expect(settingsPage.healthcheckCheckbox).toBeChecked({ checked: !wasChecked });

    // Restore
    await settingsPage.healthcheckCheckbox.setChecked(wasChecked);
    await expect(settingsPage.healthcheckCheckbox).toBeChecked({ checked: wasChecked });
  });


  test('should open, fill, and cancel Add Path modal', async ({ settingsPage }) => {
    await expect(settingsPage.pathsHeading).toBeVisible();
    await expect(settingsPage.addPathButton).toBeVisible();

    await settingsPage.openAddPathModal();
    await expect(settingsPage.pathModal.modal).toHaveClass(/active/);
    await expect(settingsPage.pathModal.heading).toBeVisible();

    // Verify inputs
    await expect(settingsPage.pathModal.serverSelect).toBeVisible();
    await expect(settingsPage.pathModal.nameInput).toBeVisible();
    await expect(settingsPage.pathModal.descriptionInput).toBeVisible();

    // Fill path details
    await settingsPage.pathModal.fill({
      pathName: 'APM_LOGS_DIR',
      pathDescription: '/var/log/service-pulse'
    });

    await expect(settingsPage.pathModal.nameInput).toHaveValue('APM_LOGS_DIR');
    await expect(settingsPage.pathModal.descriptionInput).toHaveValue('/var/log/service-pulse');

    // Cancel modal
    await settingsPage.pathModal.cancel();
    await expect(settingsPage.pathModal.modal).not.toHaveClass(/active/);
  });

  test('should open, fill, and dismiss Add Script modal', async ({ page, settingsPage }) => {
    await expect(settingsPage.scriptsHeading).toBeVisible();
    await expect(settingsPage.addScriptButton).toBeVisible();

    await settingsPage.openAddScriptModal();
    await expect(settingsPage.scriptModal.modal).toHaveClass(/active/);
    await expect(settingsPage.scriptModal.heading).toBeVisible();

    // Fill script details
    await settingsPage.scriptModal.fill({
      scriptName: 'healthcheck_probe',
      scriptExtension: 'PY'
    });

    await expect(settingsPage.scriptModal.nameInput).toHaveValue('healthcheck_probe');
    await expect(settingsPage.scriptModal.extensionSelect).toHaveValue('PY');

    // Dismiss via ESC
    await page.keyboard.press('Escape');
    await expect(settingsPage.scriptModal.modal).not.toHaveClass(/active/);
  });
});
