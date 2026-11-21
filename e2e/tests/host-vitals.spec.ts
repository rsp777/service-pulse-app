import { test, expect } from '@playwright/test';
import { openIsolatedApp } from '../src/fixtures/isolated-app';

const sample = (serverId = 1) => ({ serverId, available: true, cpuPercent: 12.5,
  memoryPercent: 40, diskPercent: 80, uptimeSeconds: 90061 });

// Issue #33: Real card rendering with isolated SSH API responses.
test('shows CPU, RAM, root disk and uptime without repeating the hostname in the heading', async ({ page }) => {
  const app = await openIsolatedApp(page);
  app.beforeApi = async (route, url) => {
    if (!url.pathname.endsWith('/metrics')) return false;
    await route.fulfill({ json: sample() }); return true;
  };
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect(page.locator('#selected-title')).toHaveText('Active server');
  await expect(page.locator('#server-metrics-display')).toBeVisible();
  await expect(page.locator('#metrics-cpu-percent')).toHaveText('CPU: 12.5%');
  await expect(page.locator('#metrics-mem-percent')).toHaveText('Memory: 40.0%');
  await expect(page.locator('#metrics-disk-percent')).toHaveText('Disk (/): 80.0%');
  await expect(page.locator('#metrics-uptime')).toHaveText('Uptime: 1d 1h 1m');
});

test('unavailable measurements show an explicit state rather than zero usage', async ({ page }) => {
  const app = await openIsolatedApp(page);
  app.beforeApi = async (route, url) => {
    if (!url.pathname.endsWith('/metrics')) return false;
    await route.fulfill({ json: { available: false, message: 'Metrics unavailable.' } }); return true;
  };
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect(page.locator('#metrics-status')).toHaveText('Metrics unavailable.');
  await expect(page.locator('#metrics-cpu-percent')).toHaveText('CPU: —');
});

test('a host change clears old metrics and a slow response cannot replace the new host sample', async ({ page }) => {
  const app = await openIsolatedApp(page);
  let release!: () => void;
  const gate = new Promise<void>(resolve => { release = resolve; });
  app.beforeApi = async (route, url) => {
    if (!url.pathname.endsWith('/metrics')) return false;
    const id = url.pathname.includes('/servers/1/') ? 1 : 2;
    if (id === 1) await gate;
    try { await route.fulfill({ json: { ...sample(id), cpuPercent: id === 1 ? 12.5 : 77 } }); } catch {}
    return true;
  };
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect(page.locator('#metrics-status')).toHaveText('Loading metrics...');
  await page.locator('#server-select').selectOption('2');
  await expect(page.locator('#metrics-cpu-percent')).toHaveText('CPU: 77.0%');
  release();
  await expect(page.locator('#metrics-cpu-percent')).toHaveText('CPU: 77.0%');
});

test('clearing the host removes readings and polling', async ({ page }) => {
  await openIsolatedApp(page);
  await page.goto('http://pulse.test/service-pulse-app/');
  await page.locator('#server-select').selectOption('');
  await expect(page.locator('#metrics-status')).toHaveText('Select a server to view metrics.');
  await expect(page.locator('#metrics-uptime')).toHaveText('Uptime: —');
});

test('refreshes telemetry after 15 seconds without changing host', async ({ page }) => {
  await page.clock.install();
  const app = await openIsolatedApp(page);
  let count = 0;
  app.beforeApi = async (route, url) => {
    if (!url.pathname.endsWith('/metrics')) return false;
    await route.fulfill({ json: { ...sample(), cpuPercent: ++count } }); return true;
  };
  await page.goto('http://pulse.test/service-pulse-app/');
  await expect(page.locator('#metrics-cpu-percent')).toHaveText('CPU: 1.0%');
  await page.clock.fastForward(15001);
  await expect(page.locator('#metrics-cpu-percent')).toHaveText('CPU: 2.0%');
});
