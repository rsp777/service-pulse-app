import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

/**
 * SidebarNavComponent — encapsulated component for the sidebar navigation links.
 * Employs Playwright's role-based locator strategy (navigation -> button[name]).
 */
export class SidebarNavComponent extends BaseComponent {
  readonly nav: Locator;
  readonly brandTitle: Locator;
  readonly dashboardButton: Locator;
  readonly serversButton: Locator;
  readonly servicesButton: Locator;
  readonly commandsButton: Locator;
  readonly alertsButton: Locator;
  readonly logsButton: Locator;
  readonly settingsButton: Locator;

  constructor(page: Page) {
    super(page);
    this.nav = page.getByRole('navigation');
    this.brandTitle = page.getByRole('heading', { name: 'Service Pulse', level: 1 });
    this.dashboardButton = this.nav.getByRole('button', { name: /Dashboard/i });
    this.serversButton   = this.nav.getByRole('button', { name: /Servers/i });
    this.servicesButton  = this.nav.getByRole('button', { name: /Services/i });
    this.commandsButton  = this.nav.getByRole('button', { name: /Commands/i });
    this.alertsButton    = this.nav.getByRole('button', { name: /Alerts/i });
    this.logsButton      = this.nav.getByRole('button', { name: /System Logs/i });
    this.settingsButton  = this.nav.getByRole('button', { name: /Settings/i });
  }

  async gotoDashboard() {
    await this.dashboardButton.click();
  }

  async gotoServers() {
    await this.serversButton.click();
  }

  async gotoServices() {
    await this.servicesButton.click();
  }

  async gotoCommands() {
    await this.commandsButton.click();
  }

  async gotoAlerts() {
    await this.alertsButton.click();
  }

  async gotoSystemLogs() {
    await this.logsButton.click();
  }

  async gotoSettings() {
    await this.settingsButton.click();
  }
}
