import { test, expect } from '@playwright/test';
import { openIsolatedApp } from '../src/fixtures/isolated-app';

// Issue #32: Positive, cancellation and stale-host regressions with real UI scripts.
test('UP disables Start and DOWN disables Stop and Restart', async ({ page }) => {
  await openIsolatedApp(page);
  await page.goto('http://pulse.test/service-pulse-app/');
  const up = page.locator('[data-service-id="1"]');
  const down = page.locator('[data-service-id="2"]');
  await expect(up.getByRole('button', { name: 'Start', exact: true })).toBeDisabled();
  await expect(up.getByRole('button', { name: 'Stop', exact: true })).toBeEnabled();
  await expect(down.getByRole('button', { name: 'Stop', exact: true })).toBeDisabled();
  await expect(down.getByRole('button', { name: 'Restart', exact: true })).toBeDisabled();
});

test('UNKNOWN is distinct from stopped and permits an explicitly confirmed stop', async ({ page }) => {
  const app = await openIsolatedApp(page);
  app.statuses[1][0] = 'UNKNOWN';
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect(page.locator('#indicator-1')).toHaveText('UNKNOWN');
  await expect(page.locator('#indicator-1')).toHaveClass(/unknown/);
  const stop = page.locator('[data-service-id="1"] [data-service-action="stop"]');
  await expect(stop).toBeEnabled();
  page.once('dialog', async dialog => { expect(dialog.message()).toContain('alpha'); await dialog.accept(); });
  await stop.click();
  await expect.poll(() => app.requests.some(r => r.url.includes('stop-service?serverId=1&serviceId=1'))).toBe(true);
});

for (const action of ['stop', 'restart']) {
  test('cancelling ' + action + ' makes no lifecycle request', async ({ page }) => {
    const app = await openIsolatedApp(page);
    await page.goto('http://pulse.test/service-pulse-app/');
    page.once('dialog', dialog => dialog.dismiss());
    await page.locator('[data-service-id="1"] [data-service-action="' + action + '"]').click();
    expect(app.requests.filter(r => r.url.includes('/api/manage-services/'))).toHaveLength(0);
  });
}

for (const action of ['stop', 'restart']) {
  test('cancelling bulk ' + action + ' makes no SSE mutation request', async ({ page }) => {
    const app = await openIsolatedApp(page);
    await page.goto('http://pulse.test/service-pulse-app/');
    page.once('dialog', dialog => dialog.dismiss());
    await page.locator('#' + action + '-all-services').click();
    expect(app.requests.filter(r => r.url.includes('/api/manage-services/'))).toHaveLength(0);
  });
}

test('a delayed host switch immediately removes old controls and ignores obsolete responses', async ({ page }) => {
  const app = await openIsolatedApp(page);
  let release!: () => void;
  const gate = new Promise<void>(resolve => { release = resolve; });
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect(page.locator('[data-service-id="1"] [data-service-action="stop"]')).toBeEnabled();
  app.beforeApi = async (_route, url) => {
    if (url.pathname.endsWith('/service-health-status/server/2')) await gate;
    return false;
  };
  await page.locator('#server-select').selectOption('2');
  await expect(page.locator('[data-service-action="stop"]')).toHaveCount(0);
  await page.locator('#server-select').selectOption('1');
  release();
  await expect(page.locator('[data-service-id="1"] [data-service-action="start"]')).toBeDisabled();
  expect(app.requests.filter(r => r.url.includes('/api/manage-services/'))).toHaveLength(0);
});
