import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

/**
 * StatsOverviewComponent — encapsulates the 4 summary operational KPI cards on the dashboard.
 */
export class StatsOverviewComponent extends BaseComponent {
  readonly registeredServersCount: Locator;
  readonly servicesOnlineCount: Locator;
  readonly activeAlertsCount: Locator;
  readonly sshStatus: Locator;

  constructor(page: Page) {
    super(page);
    this.registeredServersCount = page.locator('#stat-servers-count');
    this.servicesOnlineCount     = page.locator('#stat-services-count');
    this.activeAlertsCount       = page.locator('#stat-alerts-count');
    this.sshStatus               = page.locator('#stat-ssh-status');
  }

  async getServersCount(): Promise<string> {
    return (await this.registeredServersCount.textContent())?.trim() || '';
  }

  async getServicesCount(): Promise<string> {
    return (await this.servicesOnlineCount.textContent())?.trim() || '';
  }

  async getAlertsCount(): Promise<string> {
    return (await this.activeAlertsCount.textContent())?.trim() || '';
  }

  async getSshStatus(): Promise<string> {
    return (await this.sshStatus.textContent())?.trim() || '';
  }
}
