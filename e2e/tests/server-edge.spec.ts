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

  test('should cycle through all server status states (ONLINE, OFFLINE, UNKNOWN)', async ({ serversPage }) => {
    await serversPage.openAddServerModal();

    await serversPage.serverModal.statusSelect.selectOption('ONLINE');
    await expect(serversPage.serverModal.statusSelect).toHaveValue('ONLINE');

    await serversPage.serverModal.statusSelect.selectOption('OFFLINE');
    await expect(serversPage.serverModal.statusSelect).toHaveValue('OFFLINE');

    await serversPage.serverModal.statusSelect.selectOption('UNKNOWN');
    await expect(serversPage.serverModal.statusSelect).toHaveValue('UNKNOWN');

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
