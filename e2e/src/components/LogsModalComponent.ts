import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

/**
 * LogsModalComponent — modal dialog for live SSE streaming of service logs.
 * Issue #31: Live service log modal controls and padding verification [Author: Ravindra Singh Pawar / Antigravity]
 */
export class LogsModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly terminalBody: Locator;
  readonly closeButton: Locator;
  readonly footerCloseButton: Locator;
  readonly filterTextInput: Locator;
  readonly levelFilterSelect: Locator;
  readonly autoScrollCheckbox: Locator;
  readonly pauseButton: Locator;
  readonly copyButton: Locator;
  readonly clearButton: Locator;
  readonly streamStatus: Locator;
  readonly lineCount: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#logs-modal');
    this.heading = this.modal.locator('#service-logs-modal-title');
    this.terminalBody = page.locator('#logs-output');
    this.closeButton = this.modal.locator('.close');
    this.footerCloseButton = this.modal.locator('.modal-footer button[data-close="logs-modal"]');
    this.filterTextInput = page.locator('#service-log-filter-text');
    this.levelFilterSelect = page.locator('#service-log-level-filter');
    this.autoScrollCheckbox = page.locator('#service-log-autoscroll');
    this.pauseButton = page.locator('#service-log-pause-btn');
    this.copyButton = page.locator('#service-log-copy-btn');
    this.clearButton = page.locator('#service-log-clear-btn');
    this.streamStatus = page.locator('#service-log-stream-status');
    this.lineCount = page.locator('#service-log-line-count');
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

  async closeViaFooter() {
    await this.footerCloseButton.click();
  }
}
