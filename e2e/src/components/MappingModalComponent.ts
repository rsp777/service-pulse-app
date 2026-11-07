import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

export interface MappingFormData {
  key: string;
  value: string;
  description?: string;
}

/**
 * MappingModalComponent — modal dialog for adding key-value condition mappings.
 */
export class MappingModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly keyInput: Locator;
  readonly valueInput: Locator;
  readonly descInput: Locator;
  readonly submitButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#mapping-modal');
    this.heading = this.modal.getByRole('heading', { name: /Add Condition Mapping/i });
    this.keyInput = this.modal.locator('#mapping-key');
    this.valueInput = this.modal.locator('#mapping-value');
    this.descInput = this.modal.locator('#mapping-desc');
    this.submitButton = this.modal.getByRole('button', { name: 'Create Mapping' });
    this.cancelButton = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton = this.modal.getByRole('button', { name: '×' });
  }

  async isVisible(): Promise<boolean> {
    return this.modal.evaluate((el) => el.classList.contains('active'));
  }

  async fill(data: MappingFormData) {
    await this.keyInput.fill(data.key);
    await this.valueInput.fill(data.value);
    if (data.description) {
      await this.descInput.fill(data.description);
    }
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
