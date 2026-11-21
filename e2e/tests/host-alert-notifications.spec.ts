import { test, expect } from '@playwright/test';
import { openIsolatedApp } from '../src/fixtures/isolated-app';

const event = { id: 4, message: 'Node beta is offline', status: 'TRIGGERED', triggeredDttm: '2026-11-21T12:00:00' };

// Issue #34: Notifications remain live outside the Alerts page and handle races/failures.
test('polls the unread badge and dropdown while viewing Settings', async ({ page }) => {
  await page.clock.install();
  const app = await openIsolatedApp(page);
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect.poll(() => app.requests.filter(r => r.url.endsWith('/events/unread')).length).toBe(1);
  await page.locator('[data-page="settings"]').click();
  app.unread = [event];
  await page.clock.fastForward(10001);
  await expect(page.locator('#notification-badge')).toHaveText('1');
  await page.locator('#notification-bell').click();
  await expect(page.locator('#notification-list')).toContainText(event.message);
});

test('a transient polling failure retains the last unread state', async ({ page }) => {
  await page.clock.install();
  const app = await openIsolatedApp(page); app.unread = [event];
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect(page.locator('#notification-badge')).toHaveText('1');
  app.beforeApi = async (route, url) => {
    if (!url.pathname.endsWith('/events/unread')) return false;
    await route.fulfill({ status: 503, json: { message: 'offline' } }); return true;
  };
  await page.clock.fastForward(10001);
  await expect.poll(() => app.requests.filter(r => r.url.endsWith('/events/unread')).length).toBe(2);
  await expect(page.locator('#notification-badge')).toHaveText('1');
});

test('a slow periodic poll never starts another periodic poll in parallel', async ({ page }) => {
  await page.clock.install();
  const app = await openIsolatedApp(page);
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect.poll(() => app.requests.filter(r => r.url.endsWith('/events/unread')).length).toBe(1);
  let release!: () => void;
  const gate = new Promise<void>(resolve => { release = resolve; });
  app.beforeApi = async (route, url) => {
    if (!url.pathname.endsWith('/events/unread')) return false;
    await gate; await route.fulfill({ json: [] }); return true;
  };
  await page.clock.fastForward(10001);
  await expect.poll(() => app.requests.filter(r => r.url.endsWith('/events/unread')).length).toBe(2);
  await page.clock.fastForward(50000);
  expect(app.requests.filter(r => r.url.endsWith('/events/unread'))).toHaveLength(2);
  release();
});

test('a stale poll cannot restore an event after it is marked read', async ({ page }) => {
  await page.clock.install();
  const app = await openIsolatedApp(page); app.unread = [event];
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect(page.locator('#notification-badge')).toHaveText('1');
  let release!: () => void;
  const gate = new Promise<void>(resolve => { release = resolve; });
  let hold = true;
  app.beforeApi = async (route, url) => {
    if (url.pathname.endsWith('/events/unread') && hold) {
      hold = false; await gate; await route.fulfill({ json: [event] }); return true;
    }
    if (url.pathname.endsWith('/events/4/read')) {
      app.unread = []; await route.fulfill({ json: { success: true } }); return true;
    }
    return false;
  };
  await page.clock.fastForward(10001);
  await expect.poll(() => app.requests.filter(r => r.url.endsWith('/events/unread')).length).toBe(2);
  await page.locator('#notification-bell').click();
  await page.locator('[data-mark-id="4"]').click();
  await expect(page.locator('#notification-badge')).toBeHidden();
  const oldResponse = page.waitForResponse(response => response.url().endsWith('/events/unread'));
  release(); await oldResponse;
  await expect(page.locator('#notification-badge')).toBeHidden();
});

test('server alert form sends SERVER_STATUS and resource threshold contracts', async ({ page }) => {
  const app = await openIsolatedApp(page);
  await page.goto('http://pulse.test/service-pulse-app/');
  await page.locator('[data-page="alerts"]').click();
  await page.locator('#add-alert').click();
  await page.locator('#alert-name').fill('Host offline');
  await page.locator('#alert-server').selectOption('1');
  await expect(page.locator('#alert-condition-type')).toHaveValue('SERVER_STATUS');
  await page.locator('#alert-condition-value').fill('OFFLINE');
  const posted = page.waitForRequest(request => request.method() === 'POST' && request.url().endsWith('/api/alerts'));
  await page.locator('#alert-form button[type="submit"]').click();
  expect((await posted).postDataJSON()).toMatchObject({ targetType: 'SERVER', serverId: 1, serviceId: null, conditionType: 'SERVER_STATUS' });
  await expect(page.locator('#alert-modal')).not.toHaveClass(/active/);
  await page.locator('#add-alert').click();
  await page.locator('#alert-name').fill('Disk usage');
  await page.locator('#alert-server').selectOption('1');
  await page.locator('#alert-condition-type').selectOption('RESOURCE_USAGE');
  await page.locator('#alert-operator').selectOption('GREATER_THAN');
  await page.locator('#alert-condition-value').fill('DISK:90');
  const resource = page.waitForRequest(request => request.method() === 'POST' && request.url().endsWith('/api/alerts'));
  await page.locator('#alert-form button[type="submit"]').click();
  expect((await resource).postDataJSON()).toMatchObject({ conditionType: 'RESOURCE_USAGE', operator: 'GREATER_THAN', conditionValue: 'DISK:90' });
});
