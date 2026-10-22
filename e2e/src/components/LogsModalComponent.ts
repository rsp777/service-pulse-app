import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

/**
 * LogsModalComponent — modal dialog for live SSE streaming of service logs.
 */
export class LogsModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly terminalBody: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#logs-modal');
    this.heading = this.modal.locator('h3');
    this.terminalBody = page.locator('#logs-output');
    this.closeButton = this.modal.locator('.close');
  }

  async isVisible(): Promise<boolean> {
    return this.modal.evaluate((el) => el.classList.contains('active'));
  }

  async getLogText(): Promise<string> {
    return (await this.terminalBody.textContent()) || '';
  }

  async close() {
    await this.closeButton.click();
  }
}
