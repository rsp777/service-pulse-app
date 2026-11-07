import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

export interface ScriptFormData {
  scriptName: string;
  scriptExtension: 'SH' | 'BASH' | 'PY';
}

/**
 * ScriptModalComponent — modal dialog for configuring lifecycle execution scripts.
 */
export class ScriptModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly nameInput: Locator;
  readonly extensionSelect: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#script-modal');
    this.heading = this.modal.getByRole('heading', { name: /Execution Script/i });
    this.nameInput = this.modal.locator('#script-name');
    this.extensionSelect = this.modal.locator('#script-extension');
    this.saveButton = this.modal.getByRole('button', { name: 'Save Script' });
    this.cancelButton = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton = this.modal.getByRole('button', { name: '×' });
  }

  async isVisible(): Promise<boolean> {
    return this.modal.evaluate((el) => el.classList.contains('active'));
  }

  async fill(data: ScriptFormData) {
    await this.nameInput.fill(data.scriptName);
    await this.extensionSelect.selectOption(data.scriptExtension);
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
