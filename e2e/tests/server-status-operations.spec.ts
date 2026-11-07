import { test, expect } from '../src/fixtures/test-fixtures';

test.describe('Server Status Operational Guardrails — Service Lifecycle & Command Execution', () => {

  const onlineServerData = {
    id: 101,
    hostname: 'worker-node-01.internal',
    ipAddress: '10.200.1.10',
    osType: 'Linux',
    status: 'ONLINE',
    services: [
      { id: 301, serviceName: 'payment-service', serviceType: 'SPRING_BOOT' },
      { id: 302, serviceName: 'inventory-service', serviceType: 'SPRING_BOOT' }
    ]
  };

  const offlineServerData = {
    id: 102,
    hostname: 'standby-node-02.internal',
    ipAddress: '10.200.1.20',
    osType: 'Linux',
    status: 'OFFLINE',
    services: [
      { id: 303, serviceName: 'reporting-service', serviceType: 'SPRING_BOOT' },
      { id: 304, serviceName: 'archival-service', serviceType: 'SPRING_BOOT' }
    ]
  };

  test.describe('ONLINE Server Operations', () => {
    test.beforeEach(async ({ page, dashboardPage }) => {
      // Mock server list with ONLINE server
      await page.route('**/api/servers', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: [onlineServerData]
            })
          });
        } else {
          await route.continue();
        }
      });

      await page.route(`**/api/service-health-status/server/${onlineServerData.id}`, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: [
              { id: 1, service: { id: 301 }, status: 'UP' },
              { id: 2, service: { id: 302 }, status: 'UP' }
            ]
          })
        });
      });

      await page.route(`**/api/server-service-configurations/server/${onlineServerData.id}`, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [] })
        });
      });

      await dashboardPage.goto();
    });

    test('should allow starting service when server is ONLINE and display output in APM terminal', async ({ page, dashboardPage }) => {
      // Intercept service start API call
      let startRequested = false;
      await page.route(/\/api\/manage-services\/start-service/, async (route) => {
        startRequested = true;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Service is already running',
            data: 'Service payment-service is already running'
          })
        });
      });

      // Verify server is selected and status shows ONLINE
      await expect(dashboardPage.selectedTitle).toContainText('worker-node-01.internal');
      await expect(dashboardPage.selectedMeta).toContainText('ONLINE');

      // Trigger Start action on payment-service
      const startBtn = dashboardPage.getServiceAction(301, 'start');
      await expect(startBtn).toBeVisible();
      await startBtn.click();

      // Verify request was dispatched
      expect(startRequested).toBe(true);

      // Verify APM terminal reflects execution output and notice shows completion
      await expect(dashboardPage.terminal.output).toContainText('Service payment-service is already running');
      await expect(dashboardPage.notice).toContainText('start completed successfully');
    });

    test('should allow stopping service when server is ONLINE and update APM terminal', async ({ page, dashboardPage }) => {
      // Intercept service stop API call
      let stopRequested = false;
      await page.route(/\/api\/manage-services\/stop-service/, async (route) => {
        stopRequested = true;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Service stopped successfully',
            data: 'Stopped payment-service on worker-node-01.internal [PID: 4120 terminated]'
          })
        });
      });

      // Trigger Stop action on payment-service
      const stopBtn = dashboardPage.getServiceAction(301, 'stop');
      await expect(stopBtn).toBeVisible();
      await stopBtn.click();

      expect(stopRequested).toBe(true);
      await expect(dashboardPage.terminal.output).toContainText('Stopped payment-service on worker-node-01.internal');
      await expect(dashboardPage.notice).toContainText('stop completed successfully');
    });

    test('should allow restarting service when server is ONLINE (bulk restart stream)', async ({ page, dashboardPage }) => {
      // Intercept bulk restart stream
      await page.route(/\/api\/manage-services\/restart-all-service\/stream/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'text/event-stream',
          headers: {
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive'
          },
          body: [
            'event: output\ndata: [STOPPING] Stopping all services...\n\n',
            'event: output\ndata: [STARTING] Starting all services...\n\n',
            'event: complete\ndata: Lifecycle action completed\n\n'
          ].join('')
        });
      });

      // Click Restart All button
      await expect(dashboardPage.restartAllServicesButton).toBeVisible();
      await dashboardPage.restartAllServicesButton.click();

      // Verify streaming terminal output received lifecycle progression
      await expect(dashboardPage.terminal.output).toContainText('Stopping all services');
      await expect(dashboardPage.terminal.output).toContainText('Starting all services');
      await expect(dashboardPage.terminal.output).toContainText('[RESTART ALL COMPLETED]');
      await expect(dashboardPage.notice).toContainText('restart all services completed');
    });

    test('should execute custom command when server is ONLINE and render command output', async ({ page, dashboardPage }) => {
      const testCommand = 'echo "HEALTH_CHECK_OK"';
      let commandExecuted = false;

      await page.route(/\/api\/commands\/execute/, async (route) => {
        commandExecuted = true;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Command completed',
            data: 'HEALTH_CHECK_OK\nLinux worker-node-01 5.15.0-x86_64'
          })
        });
      });

      await dashboardPage.executeCommand(testCommand);

      expect(commandExecuted).toBe(true);
      await expect(dashboardPage.terminal.output).toContainText('HEALTH_CHECK_OK');
      await expect(dashboardPage.notice).toContainText('Command completed');
      await expect(dashboardPage.notice).not.toHaveClass(/error/);
    });

  });

  test.describe('OFFLINE Server Operations Guardrails', () => {
    test.beforeEach(async ({ page, dashboardPage }) => {
      // Mock server list with OFFLINE server
      await page.route('**/api/servers', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: [offlineServerData]
            })
          });
        } else {
          await route.continue();
        }
      });

      await page.route(`**/api/service-health-status/server/${offlineServerData.id}`, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: [
              { id: 3, service: { id: 303 }, status: 'DOWN' },
              { id: 4, service: { id: 304 }, status: 'DOWN' }
            ]
          })
        });
      });

      await page.route(`**/api/server-service-configurations/server/${offlineServerData.id}`, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [] })
        });
      });

      await dashboardPage.goto();
    });

    test('should prevent starting service when server is OFFLINE and return "Server is OFFLINE"', async ({ page, dashboardPage }) => {
      // Intercept start service request - backend contract returns "Server is OFFLINE"
      let intercepted = false;
      await page.route(/\/api\/manage-services\/start-service/, async (route) => {
        intercepted = true;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Server is OFFLINE',
            data: 'Server is OFFLINE'
          })
        });
      });

      // Verify server status badge reflects OFFLINE
      await expect(dashboardPage.selectedTitle).toContainText('standby-node-02.internal');
      await expect(dashboardPage.selectedMeta).toContainText('OFFLINE');

      // Click Start on reporting-service
      const startBtn = dashboardPage.getServiceAction(303, 'start');
      await expect(startBtn).toBeVisible();
      await startBtn.click();

      expect(intercepted).toBe(true);

      // Verify APM terminal explicitly reports "Server is OFFLINE"
      await expect(dashboardPage.terminal.output).toContainText('Server is OFFLINE');
    });

    test('should prevent stopping service when server is OFFLINE and return "Server is OFFLINE"', async ({ page, dashboardPage }) => {
      let intercepted = false;
      await page.route(/\/api\/manage-services\/stop-service/, async (route) => {
        intercepted = true;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Server is OFFLINE',
            data: 'Server is OFFLINE'
          })
        });
      });

      // Click Stop on reporting-service
      const stopBtn = dashboardPage.getServiceAction(303, 'stop');
      await expect(stopBtn).toBeVisible();
      await stopBtn.click();

      expect(intercepted).toBe(true);

      // Verify APM terminal explicitly reports "Server is OFFLINE"
      await expect(dashboardPage.terminal.output).toContainText('Server is OFFLINE');
    });

    test('should prevent restarting service when server is OFFLINE and stream "Server is OFFLINE"', async ({ page, dashboardPage }) => {
      // In streamAllServices (ManageServicesImpl.java), when server is not ONLINE:
      // lineConsumer.accept("Server is OFFLINE"); return;
      await page.route(/\/api\/manage-services\/restart-all-service\/stream/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'text/event-stream',
          headers: {
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive'
          },
          body: [
            'event: output\ndata: Server is OFFLINE\n\n',
            'event: complete\ndata: Lifecycle action completed\n\n'
          ].join('')
        });
      });

      await expect(dashboardPage.restartAllServicesButton).toBeVisible();
      await dashboardPage.restartAllServicesButton.click();

      // Terminal should display "Server is OFFLINE"
      await expect(dashboardPage.terminal.output).toContainText('Server is OFFLINE');
    });

    test('should reject/fail custom command execution when server is OFFLINE and display error banner', async ({ page, dashboardPage }) => {
      const offlineCommand = 'systemctl status reporting-service';
      let commandAttempted = false;

      // When server is OFFLINE, backend command execution throws exception or returns 500 error
      await page.route(/\/api\/commands\/execute/, async (route) => {
        commandAttempted = true;
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Host unreachable: server is OFFLINE (standby-node-02.internal)',
            data: null
          })
        });
      });


      await dashboardPage.executeCommand(offlineCommand);

      expect(commandAttempted).toBe(true);

      // Verify error notice appears with error styling
      await expect(dashboardPage.notice).toHaveClass(/error/);
      await expect(dashboardPage.notice).toContainText(/Command failed: Host unreachable: server is OFFLINE/i);

      // Verify terminal records [ERROR] output
      await expect(dashboardPage.terminal.output).toContainText('[ERROR]: Host unreachable: server is OFFLINE');
    });
  });

  test.describe('Backend Integration API Contract Validation', () => {
    test('backend API should reject start-service with "Server is OFFLINE" when targeting an offline server', async ({ request }) => {
      // Query servers to check if an OFFLINE server exists or create a temporary OFFLINE server
      const serversRes = await request.get('api/servers');
      expect(serversRes.ok()).toBeTruthy();
      const serversData = await serversRes.json();
      const servers = serversData.data || serversData;

      let offlineServer = Array.isArray(servers) ? servers.find((s: any) => s.status === 'OFFLINE') : null;

      if (!offlineServer) {
        // Create an OFFLINE server for boundary testing
        const createRes = await request.post('api/servers', {
          data: {
            hostname: 'test-offline-vault.qa',
            ipAddress: '192.168.254.99',
            osType: 'Linux',
            status: 'OFFLINE'
          }
        });
        if (createRes.ok()) {
          const created = await createRes.json();
          offlineServer = created.data || created;
        }
      }

      if (offlineServer && offlineServer.id) {
        // Attempt start-service against offline server
        const startRes = await request.post(`api/manage-services/start-service?serverId=${offlineServer.id}&serviceId=1`);
        expect(startRes.ok()).toBeTruthy();
        const startBody = await startRes.json();
        expect(startBody.data).toBe('Server is OFFLINE');

        // Attempt stop-service against offline server
        const stopRes = await request.post(`api/manage-services/stop-service?serverId=${offlineServer.id}&serviceId=1`);
        expect(stopRes.ok()).toBeTruthy();
        const stopBody = await stopRes.json();
        expect(stopBody.data).toBe('Server is OFFLINE');

        // Attempt command execution against offline server - should fail
        const cmdRes = await request.post(`api/commands/execute?serverId=${offlineServer.id}&command=whoami`);
        expect(cmdRes.status()).toBe(500);
        const cmdBody = await cmdRes.json();
        expect(cmdBody.success).toBe(false);
      }
    });
  });

});
