import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

export interface ServerFormData {
  hostname: string;
  ipAddress: string;
  osType?: string;
  status?: string;
}

/**
 * ServerModalComponent — modal dialog for registering or modifying a target Linux host.
 * Employs role and label-first locators for robust, accessible automation.
 */
export class ServerModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly hostnameInput: Locator;
  readonly ipAddressInput: Locator;
  readonly osTypeSelect: Locator;
  readonly statusSelect: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#server-modal');
    this.heading = this.modal.getByRole('heading', { name: /Server Host/i });
    this.hostnameInput  = this.modal.locator('#hostname');
    this.ipAddressInput = this.modal.locator('#ip-address');
    this.osTypeSelect   = this.modal.locator('#os-type');
    this.statusSelect   = this.modal.locator('#server-status');
    this.saveButton     = this.modal.getByRole('button', { name: 'Save Server' });
    this.cancelButton   = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton    = this.modal.getByRole('button', { name: '×' });
  }

  async isVisible(): Promise<boolean> {
    return this.modal.evaluate((el) => el.classList.contains('active'));
  }

  async fill(data: ServerFormData) {
    await this.hostnameInput.fill(data.hostname);
    await this.ipAddressInput.fill(data.ipAddress);
    if (data.osType) {
      await this.osTypeSelect.fill(data.osType);
    }
    if (data.status) {
      await this.statusSelect.selectOption(data.status);
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
