import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';
import { StatsOverviewComponent } from '../components/StatsOverviewComponent';
import { TerminalComponent } from '../components/TerminalComponent';
import { ServerModalComponent } from '../components/ServerModalComponent';
import { ServiceModalComponent } from '../components/ServiceModalComponent';
import { LogsModalComponent } from '../components/LogsModalComponent';

/**
 * DashboardPage — primary operations console view.
 * Combines stats, server selection, remote command execution form, services list, and terminal.
 */
export class DashboardPage extends BasePage {
  readonly stats: StatsOverviewComponent;
  readonly terminal: TerminalComponent;
  readonly serverModal: ServerModalComponent;
  readonly serviceModal: ServiceModalComponent;
  readonly logsModal: LogsModalComponent;

  readonly serverSelect: Locator;
  readonly addServerButton: Locator;
  readonly addServiceButton: Locator;
  readonly refreshServicesButton: Locator;
  readonly startAllServicesButton: Locator;
  readonly stopAllServicesButton: Locator;
  readonly restartAllServicesButton: Locator;
  readonly commandSelect: Locator;
  readonly commandTextarea: Locator;
  readonly runCommandButton: Locator;
  readonly serviceList: Locator;
  readonly selectedTitle: Locator;
  readonly selectedMeta: Locator;
  readonly notice: Locator;

  constructor(page: Page) {
    super(page);
    this.stats        = new StatsOverviewComponent(page);
    this.terminal     = new TerminalComponent(page);
    this.serverModal  = new ServerModalComponent(page);
    this.serviceModal = new ServiceModalComponent(page);
    this.logsModal    = new LogsModalComponent(page);

    this.serverSelect          = page.getByLabel('Active Server Context');
    this.addServerButton       = page.getByRole('button', { name: '+ Add server' }).first();
    this.addServiceButton      = page.getByRole('button', { name: '+ Add service' }).first();
    this.refreshServicesButton = page.getByRole('button', { name: 'Refresh' }).first();
    this.startAllServicesButton = page.locator('#start-all-services');
    this.stopAllServicesButton  = page.locator('#stop-all-services');
    this.restartAllServicesButton = page.locator('#restart-all-services');
    this.commandSelect         = page.getByLabel('Preconfigured Command');
    this.commandTextarea       = page.getByPlaceholder('systemctl status nginx');
    this.runCommandButton      = page.getByRole('button', { name: 'Run command' });
    this.serviceList           = page.locator('#service-list');
    this.selectedTitle         = page.locator('#selected-title');
    this.selectedMeta          = page.locator('#selected-meta');
    this.notice                = page.locator('#notice');
  }

  async selectServer(hostnameOrName: string) {
    await this.serverSelect.selectOption({ label: hostnameOrName });
  }

  async executeCommand(commandStr: string) {
    await this.commandTextarea.fill(commandStr);
    await this.runCommandButton.click();
  }

  async openAddServerModal() {
    await this.addServerButton.click();
  }

  async openAddServiceModal() {
    await this.addServiceButton.click();
  }

  getServiceRow(serviceNameOrId: string | number): Locator {
    if (typeof serviceNameOrId === 'number') {
      return this.serviceList.locator(`[data-service-id="${serviceNameOrId}"]`);
    }
    return this.serviceList.locator('.service-row').filter({ hasText: serviceNameOrId });
  }

  getServiceAction(serviceNameOrId: string | number, action: 'start' | 'stop' | 'restart' | 'logs'): Locator {
    return this.getServiceRow(serviceNameOrId).locator(`[data-service-action="${action}"]`);
  }
}

