import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Settings & Runtime Configuration — Edge Cases & Modals', () => {
  test.beforeEach(async ({ settingsPage }) => {
    await settingsPage.goto();
    await settingsPage.sidebar.gotoSettings();
    await expect(settingsPage.runtimeConfigStatus).toHaveText(/Configuration Loaded/i);
  });

  test('should verify all four runtime configuration checkboxes can toggle independently', async ({ settingsPage }) => {
    const checkboxes = [
      settingsPage.healthcheckCheckbox,
      settingsPage.serviceManagementCheckbox,
      settingsPage.alertManagementCheckbox,
      settingsPage.healthStatusPopulationCheckbox,
    ];

    for (const cb of checkboxes) {
      const initial = await cb.isChecked();
      await cb.setChecked(!initial);
      await expect(cb).toBeChecked({ checked: !initial });
      // Revert
      await cb.setChecked(initial);
      await expect(cb).toBeChecked({ checked: initial });
    }
  });

  test('should cycle through all script extension types (SH, BASH, PY)', async ({ settingsPage }) => {
    await settingsPage.openAddScriptModal();

    const extensions = ['SH', 'BASH', 'PY'] as const;
    for (const ext of extensions) {
      await settingsPage.scriptModal.extensionSelect.selectOption(ext);
      await expect(settingsPage.scriptModal.extensionSelect).toHaveValue(ext);
    }

    await settingsPage.scriptModal.cancel();
  });

  test('should dismiss Path Modal and Script Modal via backdrop click', async ({ settingsPage }) => {
    // Path modal backdrop click
    await settingsPage.openAddPathModal();
    await expect(settingsPage.pathModal.modal).toHaveClass(/active/);
    await settingsPage.pathModal.modal.click({ position: { x: 10, y: 10 } });
    await expect(settingsPage.pathModal.modal).not.toHaveClass(/active/);

    // Script modal backdrop click
    await settingsPage.openAddScriptModal();
    await expect(settingsPage.scriptModal.modal).toHaveClass(/active/);
    await settingsPage.scriptModal.modal.click({ position: { x: 10, y: 10 } });
    await expect(settingsPage.scriptModal.modal).not.toHaveClass(/active/);
  });
});
