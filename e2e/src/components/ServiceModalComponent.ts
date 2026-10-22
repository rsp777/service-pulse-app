import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

export interface ServiceFormData {
  targetServer?: string;
  serviceName: string;
  serviceType?: 'SPRING_BOOT' | 'OTHER';
  logsPath?: string;
  scriptsPath?: string;
}

/**
 * ServiceModalComponent — modal dialog for registering or modifying monitored applications.
 */
export class ServiceModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly targetServerSelect: Locator;
  readonly serviceNameInput: Locator;
  readonly serviceTypeSelect: Locator;
  readonly logsPathInput: Locator;
  readonly scriptsPathInput: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#service-modal');
    this.heading = this.modal.locator('h3');
    this.targetServerSelect = this.modal.locator('#service-server');
    this.serviceNameInput   = this.modal.locator('#service-name');
    this.serviceTypeSelect  = this.modal.locator('#service-type');
    this.logsPathInput      = this.modal.locator('#logs-path');
    this.scriptsPathInput   = this.modal.locator('#scripts-path');
    this.saveButton         = this.modal.getByRole('button', { name: 'Save Service' });
    this.cancelButton       = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton        = this.modal.locator('.close');
  }

  async isVisible(): Promise<boolean> {
    return this.modal.evaluate((el) => el.classList.contains('active'));
  }

  async fill(data: ServiceFormData) {
    if (data.targetServer) {
      await this.targetServerSelect.selectOption({ label: data.targetServer });
    }
    await this.serviceNameInput.fill(data.serviceName);
    if (data.serviceType) {
      await this.serviceTypeSelect.selectOption(data.serviceType);
    }
    if (data.logsPath) {
      await this.logsPathInput.fill(data.logsPath);
    }
    if (data.scriptsPath) {
      await this.scriptsPathInput.fill(data.scriptsPath);
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
