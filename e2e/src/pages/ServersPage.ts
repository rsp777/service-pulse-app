import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';
import { ServerModalComponent } from '../components/ServerModalComponent';

/**
 * ServersPage — Server Inventory view for managing Linux hosts.
 */
export class ServersPage extends BasePage {
  readonly heading: Locator;
  readonly addServerButton: Locator;
  readonly serverTable: Locator;
  readonly serverModal: ServerModalComponent;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: 'Server Inventory', level: 2 });
    this.addServerButton = page.getByRole('button', { name: '+ Add server' }).first();
    this.serverTable = page.locator('#server-table');
    this.serverModal = new ServerModalComponent(page);
  }

  async openAddServerModal() {
    await this.addServerButton.click();
  }
}
