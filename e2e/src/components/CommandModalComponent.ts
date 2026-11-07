import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

export interface CommandFormData {
  name: string;
  parameters: string;
  description?: string;
}

/**
 * CommandModalComponent — modal dialog for adding or editing preconfigured SSH commands.
 */
export class CommandModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly nameInput: Locator;
  readonly parametersInput: Locator;
  readonly descriptionInput: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#command-modal');
    this.heading = this.modal.getByRole('heading', { name: /SSH Command/i });
    this.nameInput = this.modal.locator('#command-name');
    this.parametersInput = this.modal.locator('#command-parameters');
    this.descriptionInput = this.modal.locator('#command-description');
    this.saveButton = this.modal.getByRole('button', { name: 'Save Command' });
    this.cancelButton = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton = this.modal.getByRole('button', { name: '×' });
  }

  async isVisible(): Promise<boolean> {
    return this.modal.evaluate((el) => el.classList.contains('active'));
  }

  async fill(data: CommandFormData) {
    await this.nameInput.fill(data.name);
    await this.parametersInput.fill(data.parameters);
    if (data.description) {
      await this.descriptionInput.fill(data.description);
    }
  }

  async save() {
    await this.saveButton.click();
  }

  async cancel() {
    await this.cancelButton.click();
  }

  async close() {
    await this.closeButton.click();
  }
}
