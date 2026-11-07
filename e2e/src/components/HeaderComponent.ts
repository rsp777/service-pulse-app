import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

/**
 * HeaderComponent — manages top header interactions, accessible headings, and notifications.
 */
export class HeaderComponent extends BaseComponent {
  readonly heading: Locator;
  readonly notificationBell: Locator;
  readonly notificationBadge: Locator;
  readonly notificationDropdown: Locator;
  readonly markAllReadButton: Locator;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: 'Infrastructure & Operations', level: 2 });
    this.notificationBell = page.getByRole('button', { name: 'Notifications' });
    this.notificationBadge = page.locator('#notification-badge');
    this.notificationDropdown = page.locator('#notification-dropdown');
    this.markAllReadButton = page.getByRole('button', { name: 'Mark all as read' });
  }

  async toggleNotifications() {
    await this.notificationBell.click();
  }

  async markAllAsRead() {
    await this.markAllReadButton.click();
  }
}
