import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * SystemLogsPage — Live SSE streaming and log viewer for backend system events.
 */
export class SystemLogsPage extends BasePage {
  readonly heading: Locator;
  readonly pauseResumeButton: Locator;
  readonly clearLogsButton: Locator;
  readonly levelFilterSelect: Locator;
  readonly terminalBody: Locator;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: /Live System Logs/i, level: 2 });
    this.pauseResumeButton = page.getByRole('button', { name: /Pause Stream|Resume Stream/i });
    this.clearLogsButton   = page.locator('#clear-system-logs-btn');
    this.levelFilterSelect = page.locator('#log-level-filter');
    this.terminalBody      = page.locator('#system-logs-body');
  }

  async toggleStream() {
    await this.pauseResumeButton.click();
  }

  async clearLogs() {
    await this.clearLogsButton.click();
  }

  async filterByLevel(level: 'ALL' | 'INFO' | 'WARN' | 'ERROR' | 'DEBUG') {
    await this.levelFilterSelect.selectOption(level);
  }

  async getLogsContent(): Promise<string> {
    return (await this.terminalBody.textContent()) || '';
  }
}
