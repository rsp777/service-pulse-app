import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Alert & Condition Management — Edge Cases & Operator Matrix', () => {
  test.beforeEach(async ({ alertsPage }) => {
    await alertsPage.goto();
    await alertsPage.sidebar.gotoAlerts();
  });

  test('should test all condition types and operators combinations in alert rule modal', async ({ alertsPage }) => {
    await alertsPage.openAddAlertModal();

    // Condition Types
    const conditionTypes = ['SERVICE_STATUS', 'HTTP_STATUS', 'RESPONSE_TIME', 'RESOURCE_USAGE'] as const;
    for (const ct of conditionTypes) {
      await alertsPage.alertModal.conditionTypeSelect.selectOption(ct);
      await expect(alertsPage.alertModal.conditionTypeSelect).toHaveValue(ct);
    }

    // Operators
    const operators = ['EQUALS', 'NOT_EQUALS', 'GREATER_THAN', 'LESS_THAN'] as const;
    for (const op of operators) {
      await alertsPage.alertModal.operatorSelect.selectOption(op);
      await expect(alertsPage.alertModal.operatorSelect).toHaveValue(op);
    }

    await alertsPage.alertModal.cancel();
  });

  test('should handle numeric vs string threshold values in alert rules', async ({ alertsPage }) => {
    await alertsPage.openAddAlertModal();

    // Numeric latency threshold
    await alertsPage.alertModal.conditionValueInput.fill('1500');
    await expect(alertsPage.alertModal.conditionValueInput).toHaveValue('1500');

    // HTTP status code string
    await alertsPage.alertModal.conditionValueInput.fill('HTTP-503');
    await expect(alertsPage.alertModal.conditionValueInput).toHaveValue('HTTP-503');

    // Health state string
    await alertsPage.alertModal.conditionValueInput.fill('DOWN');
    await expect(alertsPage.alertModal.conditionValueInput).toHaveValue('DOWN');

    await alertsPage.alertModal.cancel();
  });

  test('should cycle through all alert event statuses (TRIGGERED, ACKNOWLEDGED, RESOLVED)', async ({ alertsPage }) => {
    await alertsPage.openLogEventModal();

    await alertsPage.logEventModal.statusSelect.selectOption('TRIGGERED');
    await expect(alertsPage.logEventModal.statusSelect).toHaveValue('TRIGGERED');

    await alertsPage.logEventModal.statusSelect.selectOption('ACKNOWLEDGED');
    await expect(alertsPage.logEventModal.statusSelect).toHaveValue('ACKNOWLEDGED');

    await alertsPage.logEventModal.statusSelect.selectOption('RESOLVED');
    await expect(alertsPage.logEventModal.statusSelect).toHaveValue('RESOLVED');

    await alertsPage.logEventModal.cancel();
  });

  test('should dismiss Alert Modal and Log Event Modal via backdrop click', async ({ alertsPage }) => {
    // Alert Rule modal backdrop click
    await alertsPage.openAddAlertModal();
    await expect(alertsPage.alertModal.modal).toHaveClass(/active/);
    await alertsPage.alertModal.modal.click({ position: { x: 10, y: 10 } });
    await expect(alertsPage.alertModal.modal).not.toHaveClass(/active/);

    // Log Event modal backdrop click
    await alertsPage.openLogEventModal();
    await expect(alertsPage.logEventModal.modal).toHaveClass(/active/);
    await alertsPage.logEventModal.modal.click({ position: { x: 10, y: 10 } });
    await expect(alertsPage.logEventModal.modal).not.toHaveClass(/active/);
  });
});
