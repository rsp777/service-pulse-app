import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';
import { CommandModalComponent } from '../components/CommandModalComponent';

/**
 * CommandsPage — Preconfigured command templates executed via authenticated SSH.
 */
export class CommandsPage extends BasePage {
  readonly heading: Locator;
  readonly refreshButton: Locator;
  readonly addCommandButton: Locator;
  readonly commandTable: Locator;
  readonly commandModal: CommandModalComponent;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: 'Preconfigured Commands', level: 2 });
    this.refreshButton    = page.getByRole('button', { name: 'Refresh' }).first();
    this.addCommandButton = page.getByRole('button', { name: '+ Add command' }).first();
    this.commandTable     = page.locator('#command-table');
    this.commandModal     = new CommandModalComponent(page);
  }

  async openAddCommandModal() {
    await this.addCommandButton.click();
  }
}
