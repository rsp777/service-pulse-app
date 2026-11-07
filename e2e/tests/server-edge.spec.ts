import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Server Inventory Management — Edge Cases & Validation', () => {
  test.beforeEach(async ({ serversPage }) => {
    await serversPage.goto();
    await serversPage.sidebar.gotoServers();
  });

  test('should support long fully-qualified domain names (FQDN) in hostname', async ({ serversPage }) => {
    const fqdn = 'prod-k8s-worker-node-42.us-east-2.compute.internal.enterprise.org';
    await serversPage.openAddServerModal();
    await serversPage.serverModal.hostnameInput.fill(fqdn);
    await expect(serversPage.serverModal.hostnameInput).toHaveValue(fqdn);
    await serversPage.serverModal.cancel();
  });

  test('should handle IPv4, IPv6, and localhost DNS addresses', async ({ serversPage }) => {
    await serversPage.openAddServerModal();

    // Localhost IP
    await serversPage.serverModal.ipAddressInput.fill('127.0.0.1');
    await expect(serversPage.serverModal.ipAddressInput).toHaveValue('127.0.0.1');

    // Private network IP
    await serversPage.serverModal.ipAddressInput.fill('10.244.0.15');
    await expect(serversPage.serverModal.ipAddressInput).toHaveValue('10.244.0.15');

    // IPv6 Address
    await serversPage.serverModal.ipAddressInput.fill('fe80::1ff:fe23:4567:890a');
    await expect(serversPage.serverModal.ipAddressInput).toHaveValue('fe80::1ff:fe23:4567:890a');

    await serversPage.serverModal.cancel();
  });

  // Issue #26: Test SSH configuration inputs and connection test feedback [Author: Ravindra Singh Pawar / Antigravity]
  test('should handle SSH port and username configuration and test connection feedback', async ({ page, serversPage }) => {
    await serversPage.openAddServerModal();

    // Default values
    await expect(serversPage.serverModal.sshPortInput).toHaveValue('22');
    await expect(serversPage.serverModal.sshUsernameInput).toHaveValue('ubuntu');

    // Custom SSH port and user
    await serversPage.serverModal.sshPortInput.fill('2222');
    await serversPage.serverModal.sshUsernameInput.fill('deployer');
    await expect(serversPage.serverModal.sshPortInput).toHaveValue('2222');
    await expect(serversPage.serverModal.sshUsernameInput).toHaveValue('deployer');

    // Negative case: Clicking test connection without IP displays warning
    await serversPage.serverModal.testConnection();
    await expect(serversPage.serverModal.connectionFeedback).toBeVisible();
    await expect(serversPage.serverModal.connectionFeedback).toContainText('Please enter an IP address');

    // Positive mock case: Test connection response display
    await page.route('**/api/servers/test-connection', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'SSH handshake successful',
          data: { connected: true, message: 'SSH handshake successful' }
        })
      });
    });

    await serversPage.serverModal.ipAddressInput.fill('10.0.0.1');
    await serversPage.serverModal.testConnection();
    await expect(serversPage.serverModal.connectionFeedback).toBeVisible();
    await expect(serversPage.serverModal.connectionFeedback).toContainText('SSH handshake successful');

    await serversPage.serverModal.cancel();
  });

  test('should dismiss modal when clicking outside on backdrop overlay', async ({ page, serversPage }) => {
    await serversPage.openAddServerModal();
    await expect(serversPage.serverModal.modal).toHaveClass(/active/);

    // Click on the backdrop (outside .modal-content)
    await serversPage.serverModal.modal.click({ position: { x: 10, y: 10 } });
    await expect(serversPage.serverModal.modal).not.toHaveClass(/active/);
  });

  test('should reset form state on subsequent modal openings', async ({ serversPage }) => {
    await serversPage.openAddServerModal();
    await serversPage.serverModal.hostnameInput.fill('temporary-dirty-host');
    await serversPage.serverModal.ipAddressInput.fill('192.168.99.99');
    await serversPage.serverModal.cancel();

    // Re-open modal and verify fields are cleanly reset
    await serversPage.openAddServerModal();
    await expect(serversPage.serverModal.hostnameInput).toHaveValue('');
    await expect(serversPage.serverModal.ipAddressInput).toHaveValue('');
    await serversPage.serverModal.cancel();
  });
});
