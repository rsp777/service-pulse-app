import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

export interface PathFormData {
  serverName?: string;
  pathName: string;
  pathDescription: string;
}

/**
 * PathModalComponent — modal dialog for configuring server directory paths.
 */
export class PathModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly serverSelect: Locator;
  readonly nameInput: Locator;
  readonly descriptionInput: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#path-modal');
    this.heading = this.modal.getByRole('heading', { name: /Server Path/i });
    this.serverSelect = this.modal.locator('#path-server');
    this.nameInput = this.modal.locator('#path-name');
    this.descriptionInput = this.modal.locator('#path-description');
    this.saveButton = this.modal.getByRole('button', { name: 'Save Path' });
    this.cancelButton = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton = this.modal.getByRole('button', { name: '×' });
  }

  async isVisible(): Promise<boolean> {
    return this.modal.evaluate((el) => el.classList.contains('active'));
  }

  async fill(data: PathFormData) {
    if (data.serverName) {
      await this.serverSelect.selectOption({ label: data.serverName });
    }
    await this.nameInput.fill(data.pathName);
    await this.descriptionInput.fill(data.pathDescription);
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
