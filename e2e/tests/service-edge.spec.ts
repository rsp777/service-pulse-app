import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Service Registry Management — Edge Cases & Boundary Values', () => {
  test.beforeEach(async ({ servicesPage }) => {
    await servicesPage.goto();
    await servicesPage.sidebar.gotoServices();
  });

  test('should validate port boundaries and numeric constraints on actuator port', async ({ page, servicesPage }) => {
    await servicesPage.openAddServiceModal();

    const portInput = page.locator('#actuator-port');
    await expect(portInput).toBeVisible();

    // Check min and max boundary attributes
    await expect(portInput).toHaveAttribute('type', 'number');
    await expect(portInput).toHaveAttribute('min', '1');
    await expect(portInput).toHaveAttribute('max', '65535');

    // Fill valid boundary values
    await portInput.fill('1');
    await expect(portInput).toHaveValue('1');

    await portInput.fill('65535');
    await expect(portInput).toHaveValue('65535');

    await portInput.fill('9092');
    await expect(portInput).toHaveValue('9092');

    await servicesPage.serviceModal.cancel();
  });

  test('should accept various context path formats with and without leading/trailing slashes', async ({ page, servicesPage }) => {
    await servicesPage.openAddServiceModal();

    const contextPathInput = page.locator('#context-path');

    // Standard path
    await contextPathInput.fill('/service-pulse-app');
    await expect(contextPathInput).toHaveValue('/service-pulse-app');

    // Deep nested path with trailing slash
    await contextPathInput.fill('/api/v2/microservices/');
    await expect(contextPathInput).toHaveValue('/api/v2/microservices/');

    // Path without leading slash
    await contextPathInput.fill('auth-service');
    await expect(contextPathInput).toHaveValue('auth-service');

    await servicesPage.serviceModal.cancel();
  });

  test('should handle custom Linux directory paths for logs and scripts', async ({ servicesPage }) => {
    await servicesPage.openAddServiceModal();

    const logsPath = '/var/log/custom-apps/payment-gateway-service';
    const scriptsPath = '/opt/enterprise/automation/scripts';

    await servicesPage.serviceModal.logsPathInput.fill(logsPath);
    await servicesPage.serviceModal.scriptsPathInput.fill(scriptsPath);

    await expect(servicesPage.serviceModal.logsPathInput).toHaveValue(logsPath);
    await expect(servicesPage.serviceModal.scriptsPathInput).toHaveValue(scriptsPath);

    await servicesPage.serviceModal.cancel();
  });

  test('should dismiss modal when clicking outside on backdrop', async ({ servicesPage }) => {
    await servicesPage.openAddServiceModal();
    await expect(servicesPage.serviceModal.modal).toHaveClass(/active/);

    // Click backdrop
    await servicesPage.serviceModal.modal.click({ position: { x: 10, y: 10 } });
    await expect(servicesPage.serviceModal.modal).not.toHaveClass(/active/);
  });

  // Issue #27: Test Health Check URL auto-derivation & custom manual override [Author: Ravindra Singh Pawar / Antigravity]
  test('should dynamically derive Spring Boot Actuator health URL and allow custom override', async ({ page, servicesPage }) => {
    await servicesPage.openAddServiceModal();

    const portInput = page.locator('#actuator-port');
    const ctxInput  = page.locator('#context-path');
    const healthUrlInput = page.locator('#health-check-url');

    await portInput.fill('8085');
    await expect(healthUrlInput).toHaveValue(/:8085\/actuator\/health/);

    await ctxInput.fill('payments');
    await expect(healthUrlInput).toHaveValue(/:8085\/payments\/actuator\/health/);

    // Custom manual override
    await healthUrlInput.fill('https://custom-domain.internal/healthz');
    await expect(healthUrlInput).toHaveValue('https://custom-domain.internal/healthz');

    await servicesPage.serviceModal.cancel();
  });
});
