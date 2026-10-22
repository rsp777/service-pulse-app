import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

export interface ServiceFormData {
  targetServer?: string;
  serviceName: string;
  serviceType?: 'SPRING_BOOT' | 'OTHER';
  actuatorPort?: number;
  contextPath?: string;
  healthCheckUrl?: string;
  logsPath?: string;
  scriptsPath?: string;
}

/**
 * ServiceModalComponent — modal dialog for registering or modifying monitored applications.
 * Issue #27: Added health check URL, actuator port, and context path locators [Author: Ravindra Singh Pawar / Antigravity]
 */
export class ServiceModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly targetServerSelect: Locator;
  readonly serviceNameInput: Locator;
  readonly serviceTypeSelect: Locator;
  readonly actuatorPortInput: Locator;
  readonly contextPathInput: Locator;
  readonly healthCheckUrlInput: Locator;
  readonly logsPathInput: Locator;
  readonly scriptsPathInput: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#service-modal');
    this.heading = this.modal.locator('h3');
    this.targetServerSelect   = this.modal.locator('#service-server');
    this.serviceNameInput     = this.modal.locator('#service-name');
    this.serviceTypeSelect    = this.modal.locator('#service-type');
    this.actuatorPortInput    = this.modal.locator('#actuator-port');
    this.contextPathInput     = this.modal.locator('#context-path');
    this.healthCheckUrlInput  = this.modal.locator('#health-check-url');
    this.logsPathInput        = this.modal.locator('#logs-path');
    this.scriptsPathInput     = this.modal.locator('#scripts-path');
    this.saveButton           = this.modal.getByRole('button', { name: 'Save Service' });
    this.cancelButton         = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton          = this.modal.locator('.close');
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
    if (data.actuatorPort !== undefined) {
      await this.actuatorPortInput.fill(String(data.actuatorPort));
    }
    if (data.contextPath) {
      await this.contextPathInput.fill(data.contextPath);
    }
    if (data.healthCheckUrl) {
      await this.healthCheckUrlInput.fill(data.healthCheckUrl);
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
