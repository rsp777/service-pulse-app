import { Page, Locator } from '@playwright/test';

/**
 * BaseComponent — foundational class for all accessible UI component objects.
 */
export abstract class BaseComponent {
  protected readonly page: Page;
  protected readonly root?: Locator;

  constructor(page: Page, root?: Locator) {
    this.page = page;
    this.root = root;
  }
}
