import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

export interface LogEventFormData {
  alertId: string | number;
  serverName?: string;
  serviceId?: string | number;
  condition: string;
  status: 'TRIGGERED' | 'ACKNOWLEDGED' | 'RESOLVED';
  message: string;
}

/**
 * LogEventModalComponent — modal dialog for manually recording audit alert events.
 */
export class LogEventModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly alertIdInput: Locator;
  readonly serverSelect: Locator;
  readonly serviceIdInput: Locator;
  readonly conditionInput: Locator;
  readonly statusSelect: Locator;
  readonly messageTextarea: Locator;
  readonly submitButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#log-event-modal');
    this.heading = this.modal.getByRole('heading', { name: /Log New Alert Event/i });
    this.alertIdInput = this.modal.locator('#event-log-alert-id');
    this.serverSelect = this.modal.locator('#event-log-server');
    this.serviceIdInput = this.modal.locator('#event-log-service-id');
    this.conditionInput = this.modal.locator('#event-log-condition');
    this.statusSelect = this.modal.locator('#event-log-status');
    this.messageTextarea = this.modal.locator('#event-log-message');
    this.submitButton = this.modal.getByRole('button', { name: 'Log Event' });
    this.cancelButton = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton = this.modal.getByRole('button', { name: '×' });
  }

  async isVisible(): Promise<boolean> {
    return this.modal.evaluate((el) => el.classList.contains('active'));
  }

  async fill(data: LogEventFormData) {
    await this.alertIdInput.fill(String(data.alertId));
    if (data.serverName) {
      await this.serverSelect.selectOption({ label: data.serverName });
    }
    if (data.serviceId) {
      await this.serviceIdInput.fill(String(data.serviceId));
    }
    await this.conditionInput.fill(data.condition);
    await this.statusSelect.selectOption(data.status);
    await this.messageTextarea.fill(data.message);
  }

  async submit() {
    await this.submitButton.click();
  }

  async cancel() {
    await this.cancelButton.click();
  }

  async close() {
    await this.closeButton.click();
  }
}
