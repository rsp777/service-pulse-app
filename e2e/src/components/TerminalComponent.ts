import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

/**
 * TerminalComponent — operational APM terminal console on the dashboard.
 */
export class TerminalComponent extends BaseComponent {
  readonly copyButton: Locator;
  readonly clearButton: Locator;
  readonly output: Locator;

  constructor(page: Page) {
    super(page);
    this.copyButton  = page.locator('#terminal-copy-btn');
    this.clearButton = page.locator('#terminal-clear-btn');
    this.output      = page.locator('#output');
  }

  async clear() {
    await this.clearButton.click();
  }

  async copy() {
    await this.copyButton.click();
  }

  async getOutputText(): Promise<string> {
    return (await this.output.textContent()) || '';
  }
}
