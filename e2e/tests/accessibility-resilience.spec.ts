import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Global Accessibility, Notifications & Viewport Resilience', () => {
  test.beforeEach(async ({ dashboardPage }) => {
    await dashboardPage.goto();
  });

  test('should dismiss notification dropdown when clicking outside in workspace', async ({ page, dashboardPage }) => {
    // Open dropdown
    await dashboardPage.header.toggleNotifications();
    await expect(dashboardPage.header.notificationDropdown).toHaveClass(/active/);

    // Click outside on main header title
    await page.getByRole('heading', { name: 'Infrastructure & Operations', level: 2 }).click();
    await expect(dashboardPage.header.notificationDropdown).not.toHaveClass(/active/);
  });

  test('should dismiss notification dropdown via ESC key', async ({ page, dashboardPage }) => {
    // Open dropdown
    await dashboardPage.header.toggleNotifications();
    await expect(dashboardPage.header.notificationDropdown).toHaveClass(/active/);

    // Press Escape
    await page.keyboard.press('Escape');
    await expect(dashboardPage.header.notificationDropdown).not.toHaveClass(/active/);
  });

  test('should support keyboard tab navigation through sidebar buttons', async ({ page, dashboardPage }) => {
    // Focus first navigation button
    await dashboardPage.sidebar.dashboardButton.focus();
    await expect(dashboardPage.sidebar.dashboardButton).toBeFocused();

    // Tab to next button (Servers)
    await page.keyboard.press('Tab');
    await expect(dashboardPage.sidebar.serversButton).toBeFocused();

    // Tab to Services
    await page.keyboard.press('Tab');
    await expect(dashboardPage.sidebar.servicesButton).toBeFocused();
  });

  test('should render properly across responsive tablet and mobile viewports', async ({ page }) => {
    // Tablet viewport (1024x768)
    await page.setViewportSize({ width: 1024, height: 768 });
    await expect(page.locator('.sidebar')).toBeVisible();
    await expect(page.locator('.main')).toBeVisible();

    // Mobile viewport (375x667)
    await page.setViewportSize({ width: 375, height: 667 });
    await expect(page.locator('.app')).toBeVisible();
    await expect(page.locator('.main')).toBeVisible();
  });

  test('should verify global notice status container accessibility attribute', async ({ page }) => {
    const notice = page.locator('#notice');
    await expect(notice).toHaveAttribute('role', 'status');
  });
});
