import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Alert & Condition Management View', () => {
  test.beforeEach(async ({ alertsPage }) => {
    await alertsPage.goto();
    await alertsPage.sidebar.gotoAlerts();
  });

  test('should display Alert Management heading, refresh, and add rule buttons', async ({ alertsPage }) => {
    await expect(alertsPage.heading).toBeVisible();
    await expect(alertsPage.refreshButton).toBeVisible();
    await expect(alertsPage.newAlertButton).toBeVisible();
  });

  test('should navigate between Alert Rules, Triggered Events, and Conditions tabs', async ({ alertsPage }) => {
    // Tab 1: Rules active by default
    await expect(alertsPage.rulesTabButton).toHaveClass(/active/);
    await expect(alertsPage.rulesContainer).toBeVisible();

    // Tab 2: Triggered Events
    await alertsPage.switchToEventsTab();
    await expect(alertsPage.eventsTabButton).toHaveClass(/active/);
    await expect(alertsPage.eventsContainer).toBeVisible();
    await expect(alertsPage.rulesContainer).not.toBeVisible();

    // Tab 3: Conditions & Mappings
    await alertsPage.switchToConditionsTab();
    await expect(alertsPage.conditionsTabButton).toHaveClass(/active/);
    await expect(alertsPage.conditionsContainer).toBeVisible();
    await expect(alertsPage.eventsContainer).not.toBeVisible();

    // Back to Rules
    await alertsPage.switchToRulesTab();
    await expect(alertsPage.rulesTabButton).toHaveClass(/active/);
    await expect(alertsPage.rulesContainer).toBeVisible();
  });

  test('should open, fill, and cancel Add Alert Rule modal', async ({ alertsPage }) => {
    await alertsPage.openAddAlertModal();
    await expect(alertsPage.alertModal.modal).toHaveClass(/active/);
    await expect(alertsPage.alertModal.heading).toBeVisible();

    // Verify inputs
    await expect(alertsPage.alertModal.nameInput).toBeVisible();
    await expect(alertsPage.alertModal.targetTypeSelect).toBeVisible();
    await expect(alertsPage.alertModal.conditionTypeSelect).toBeVisible();
    await expect(alertsPage.alertModal.operatorSelect).toBeVisible();
    await expect(alertsPage.alertModal.conditionValueInput).toBeVisible();
    await expect(alertsPage.alertModal.enabledCheckbox).toBeVisible();

    // Fill form
    await alertsPage.alertModal.fill({
      name: 'High CPU Rule',
      targetType: 'SERVER',
      conditionType: 'RESOURCE_USAGE',
      operator: 'GREATER_THAN',
      conditionValue: '85',
      enabled: true
    });

    await expect(alertsPage.alertModal.nameInput).toHaveValue('High CPU Rule');
    await expect(alertsPage.alertModal.conditionValueInput).toHaveValue('85');

    // Cancel modal
    await alertsPage.alertModal.cancel();
    await expect(alertsPage.alertModal.modal).not.toHaveClass(/active/);
  });

  test('should toggle server and service target groups when changing scope in alert rule modal', async ({ page, alertsPage }) => {
    await alertsPage.openAddAlertModal();

    const serverGroup = page.locator('#alert-server-group');
    const serviceGroup = page.locator('#alert-service-group');

    // Default scope is SERVER
    await expect(serverGroup).toBeVisible();
    await expect(serviceGroup).not.toBeVisible();

    // Switch scope to SERVICE
    await alertsPage.alertModal.targetTypeSelect.selectOption('SERVICE');
    await expect(serverGroup).not.toBeVisible();
    await expect(serviceGroup).toBeVisible();

    // Switch scope back to SERVER
    await alertsPage.alertModal.targetTypeSelect.selectOption('SERVER');
    await expect(serverGroup).toBeVisible();
    await expect(serviceGroup).not.toBeVisible();

    await alertsPage.alertModal.close();
    await expect(alertsPage.alertModal.modal).not.toHaveClass(/active/);
  });

  test('should open, fill, and close Log Alert Event modal from Triggered Events tab', async ({ alertsPage }) => {
    await alertsPage.openLogEventModal();
    await expect(alertsPage.logEventModal.modal).toHaveClass(/active/);
    await expect(alertsPage.logEventModal.heading).toBeVisible();

    // Fill event details
    await alertsPage.logEventModal.fill({
      alertId: 1,
      condition: 'SERVICE_DOWN',
      status: 'TRIGGERED',
      message: 'Service unreachable during automated health check'
    });

    await expect(alertsPage.logEventModal.alertIdInput).toHaveValue('1');
    await expect(alertsPage.logEventModal.conditionInput).toHaveValue('SERVICE_DOWN');

    // Cancel
    await alertsPage.logEventModal.cancel();
    await expect(alertsPage.logEventModal.modal).not.toHaveClass(/active/);
  });
});
