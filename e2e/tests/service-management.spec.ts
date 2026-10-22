import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Service Application Management & Modals', () => {
  test.beforeEach(async ({ servicesPage }) => {
    await servicesPage.goto();
    await servicesPage.sidebar.gotoServices();
  });

  test('should render service registry view and server filter selector', async ({ servicesPage }) => {
    await expect(servicesPage.heading).toBeVisible();
    await expect(servicesPage.addServiceButton).toBeVisible();
    await expect(servicesPage.filterServerSelect).toBeVisible();
    await expect(servicesPage.servicesTable).toBeVisible();
  });

  test('should open Add Service modal and display accessible form controls', async ({ page, servicesPage }) => {
    await servicesPage.openAddServiceModal();
    await expect(servicesPage.serviceModal.modal).toHaveClass(/active/);
    await expect(servicesPage.serviceModal.heading).toBeVisible();

    // Issue #27: Verify accessible inputs including healthCheckUrl and actuator fields [Author: Ravindra Singh Pawar / Antigravity]
    await expect(servicesPage.serviceModal.targetServerSelect).toBeVisible();
    await expect(servicesPage.serviceModal.serviceNameInput).toBeVisible();
    await expect(servicesPage.serviceModal.serviceTypeSelect).toBeVisible();
    await expect(servicesPage.serviceModal.healthCheckUrlInput).toBeVisible();
    await expect(servicesPage.serviceModal.logsPathInput).toBeVisible();
    await expect(servicesPage.serviceModal.scriptsPathInput).toBeVisible();

    // Fill service details including Health Check URL
    await servicesPage.serviceModal.fill({
      serviceName: 'auth-service',
      serviceType: 'SPRING_BOOT',
      actuatorPort: 8080,
      contextPath: '/auth',
      healthCheckUrl: 'http://100.87.125.44:8080/auth/actuator/health',
      logsPath: '/opt/apps/logs/auth-service',
      scriptsPath: '/opt/apps/scripts'
    });

    await expect(servicesPage.serviceModal.serviceNameInput).toHaveValue('auth-service');
    await expect(servicesPage.serviceModal.healthCheckUrlInput).toHaveValue('http://100.87.125.44:8080/auth/actuator/health');
    await expect(servicesPage.serviceModal.logsPathInput).toHaveValue('/opt/apps/logs/auth-service');

    // Cancel and ensure modal closes
    await servicesPage.serviceModal.cancel();
    await expect(servicesPage.serviceModal.modal).not.toHaveClass(/active/);
  });

  test('should toggle Spring Boot actuator fields when switching service type', async ({ page, servicesPage }) => {
    await servicesPage.openAddServiceModal();

    const springBootFields = page.locator('#spring-boot-fields');
    await expect(springBootFields).toBeVisible();

    // Switch to OTHER
    await servicesPage.serviceModal.serviceTypeSelect.selectOption('OTHER');
    await expect(springBootFields).not.toBeVisible();

    // Switch back to SPRING_BOOT
    await servicesPage.serviceModal.serviceTypeSelect.selectOption('SPRING_BOOT');
    await expect(springBootFields).toBeVisible();

    await servicesPage.serviceModal.close();
    await expect(servicesPage.serviceModal.modal).not.toHaveClass(/active/);
  });

  test('should dismiss Add Service modal via ESC keyboard press', async ({ page, servicesPage }) => {
    await servicesPage.openAddServiceModal();
    await expect(servicesPage.serviceModal.modal).toHaveClass(/active/);

    await page.keyboard.press('Escape');
    await expect(servicesPage.serviceModal.modal).not.toHaveClass(/active/);
  });
});
