import { Page, Route } from '@playwright/test';
import { readFile } from 'fs/promises';
import path from 'path';

// Issue #32: Exercise checked-in UI without touching managed hosts or a database.
export async function openIsolatedApp(page: Page) {
  const services = [{ id: 1, serviceName: 'orders' }, { id: 2, serviceName: 'billing' }];
  const app = {
    servers: [
      { id: 1, hostname: 'alpha', ipAddress: '192.0.2.1', status: 'ONLINE', services },
      { id: 2, hostname: 'beta', ipAddress: '192.0.2.2', status: 'ONLINE', services },
    ],
    statuses: { 1: ['UP', 'DOWN'], 2: ['DOWN', 'UP'] } as Record<number, string[]>,
    requests: [] as { method: string; url: string }[],
    unread: [] as any[],
    beforeApi: null as null | ((route: Route, url: URL) => Promise<boolean>),
  };
  await page.route('**/*', async route => {
    const url = new URL(route.request().url());
    if (url.hostname !== 'pulse.test') return route.abort();
    const relative = url.pathname.replace(/^\/service-pulse-app\//, '');
    if (relative.startsWith('api/')) {
      app.requests.push({ method: route.request().method(), url: route.request().url() });
      if (app.beforeApi && await app.beforeApi(route, url)) return;
      let data: any = [];
      if (relative === 'api/servers') data = app.servers;
      const health = relative.match(/^api\/service-health-status\/server\/(\d+)$/);
      if (health) data = services.map((service, i) => ({ service, status: app.statuses[Number(health[1])][i] }));
      if (relative === 'api/alerts/events/unread') data = app.unread;
      if (relative.startsWith('api/manage-services/')) data = 'done';
      return route.fulfill({ json: data });
    }
    const resource = path.resolve(__dirname, '../../../src/main/resources/static', relative || 'index.html');
    const contentType = resource.endsWith('.js') ? 'text/javascript' : resource.endsWith('.css') ? 'text/css' : 'text/html';
    try { await route.fulfill({ body: await readFile(resource), contentType }); }
    catch { await route.fulfill({ status: 404, body: '' }); }
  });
  return app;
}
