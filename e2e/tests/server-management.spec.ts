import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Server Management & Modals', () => {
  test.beforeEach(async ({ serversPage }) => {
    await serversPage.goto();
    await serversPage.sidebar.gotoServers();
  });

  test('should render server inventory table with proper headers', async ({ serversPage }) => {
    await expect(serversPage.heading).toBeVisible();
    await expect(serversPage.addServerButton).toBeVisible();
    await expect(serversPage.serverTable).toBeVisible();
  });

  test('should open and close Add Server modal with accessible role-based elements', async ({ serversPage }) => {
    await serversPage.openAddServerModal();
    await expect(serversPage.serverModal.modal).toHaveClass(/active/);
    await expect(serversPage.serverModal.heading).toBeVisible();

    // Issue #26: Verify form inputs including SSH configuration [Author: Ravindra Singh Pawar / Antigravity]
    await expect(serversPage.serverModal.hostnameInput).toBeVisible();
    await expect(serversPage.serverModal.ipAddressInput).toBeVisible();
    await expect(serversPage.serverModal.osTypeSelect).toBeVisible();
    await expect(serversPage.serverModal.sshPortInput).toBeVisible();
    await expect(serversPage.serverModal.sshUsernameInput).toBeVisible();
    await expect(serversPage.serverModal.testConnectionButton).toBeVisible();

    // Fill form fields including SSH port and username
    await serversPage.serverModal.fill({
      hostname: 'auth-wms.internal',
      ipAddress: '100.87.125.44',
      osType: 'Linux',
      sshPort: 22,
      sshUsername: 'ubuntu'
    });

    await expect(serversPage.serverModal.hostnameInput).toHaveValue('auth-wms.internal');
    await expect(serversPage.serverModal.ipAddressInput).toHaveValue('100.87.125.44');
    await expect(serversPage.serverModal.sshPortInput).toHaveValue('22');
    await expect(serversPage.serverModal.sshUsernameInput).toHaveValue('ubuntu');

    // Cancel and ensure modal closes
    await serversPage.serverModal.cancel();
    await expect(serversPage.serverModal.modal).not.toHaveClass(/active/);
  });

  test('should close Add Server modal when clicking close (x) button', async ({ serversPage }) => {
    await serversPage.openAddServerModal();
    await expect(serversPage.serverModal.modal).toHaveClass(/active/);

    await serversPage.serverModal.close();
    await expect(serversPage.serverModal.modal).not.toHaveClass(/active/);
  });

  test('should dismiss Add Server modal via ESC keyboard press', async ({ page, serversPage }) => {
    await serversPage.openAddServerModal();
    await expect(serversPage.serverModal.modal).toHaveClass(/active/);

    await page.keyboard.press('Escape');
    await expect(serversPage.serverModal.modal).not.toHaveClass(/active/);
  });

  test('should enforce HTML5 required attribute on hostname and IP address', async ({ serversPage }) => {
    await serversPage.openAddServerModal();
    await expect(serversPage.serverModal.hostnameInput).toHaveAttribute('required', '');
    await expect(serversPage.serverModal.ipAddressInput).toHaveAttribute('required', '');
    await serversPage.serverModal.cancel();
  });
});
