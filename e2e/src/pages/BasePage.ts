import { Page, Locator } from '@playwright/test';
import { SidebarNavComponent } from '../components/SidebarNavComponent';
import { HeaderComponent } from '../components/HeaderComponent';

/**
 * BasePage — parent page providing common layout components, navigation, and notice alerts.
 */
export abstract class BasePage {
  readonly page: Page;
  readonly sidebar: SidebarNavComponent;
  readonly header: HeaderComponent;
  readonly notice: Locator;

  constructor(page: Page) {
    this.page = page;
    this.sidebar = new SidebarNavComponent(page);
    this.header = new HeaderComponent(page);
    this.notice = page.getByRole('status');
  }

  async goto(path: string = '') {
    await this.page.goto(path);
  }

  async getNoticeText(): Promise<string> {
    return (await this.notice.textContent())?.trim() || '';
  }

  async waitForNotice(): Promise<string> {
    await this.notice.waitFor({ state: 'visible', timeout: 5000 });
    return this.getNoticeText();
  }
}
