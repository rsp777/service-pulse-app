import { Page, Locator } from '@playwright/test';
import { BaseComponent } from './BaseComponent';

export interface AlertRuleFormData {
  name: string;
  targetType: 'SERVER' | 'SERVICE';
  serverName?: string;
  serviceName?: string;
  conditionType: 'SERVICE_STATUS' | 'HTTP_STATUS' | 'RESPONSE_TIME' | 'RESOURCE_USAGE';
  operator: 'EQUALS' | 'NOT_EQUALS' | 'GREATER_THAN' | 'LESS_THAN';
  conditionValue: string;
  enabled?: boolean;
}

/**
 * AlertModalComponent — modal dialog for configuring alert rules.
 */
export class AlertModalComponent extends BaseComponent {
  readonly modal: Locator;
  readonly heading: Locator;
  readonly nameInput: Locator;
  readonly targetTypeSelect: Locator;
  readonly serverSelect: Locator;
  readonly serviceSelect: Locator;
  readonly conditionTypeSelect: Locator;
  readonly operatorSelect: Locator;
  readonly conditionValueInput: Locator;
  readonly enabledCheckbox: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = page.locator('#alert-modal');
    this.heading = this.modal.getByRole('heading', { name: /Alert Rule/i });
    this.nameInput = this.modal.locator('#alert-name');
    this.targetTypeSelect = this.modal.locator('#alert-target-type');
    this.serverSelect = this.modal.locator('#alert-server');
    this.serviceSelect = this.modal.locator('#alert-service');
    this.conditionTypeSelect = this.modal.locator('#alert-condition-type');
    this.operatorSelect = this.modal.locator('#alert-operator');
    this.conditionValueInput = this.modal.locator('#alert-condition-value');
    this.enabledCheckbox = this.modal.locator('#alert-enabled');
    this.saveButton = this.modal.getByRole('button', { name: 'Save Rule' });
    this.cancelButton = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton = this.modal.getByRole('button', { name: '×' });
  }

  async isVisible(): Promise<boolean> {
    return this.modal.evaluate((el) => el.classList.contains('active'));
  }

  async fill(data: AlertRuleFormData) {
    await this.nameInput.fill(data.name);
    await this.targetTypeSelect.selectOption(data.targetType);
    if (data.targetType === 'SERVER' && data.serverName) {
      await this.serverSelect.selectOption({ label: data.serverName });
    } else if (data.targetType === 'SERVICE' && data.serviceName) {
      await this.serviceSelect.selectOption({ label: data.serviceName });
    }
    await this.conditionTypeSelect.selectOption(data.conditionType);
    await this.operatorSelect.selectOption(data.operator);
    await this.conditionValueInput.fill(data.conditionValue);
    if (data.enabled !== undefined) {
      const isChecked = await this.enabledCheckbox.isChecked();
      if (isChecked !== data.enabled) {
        await this.enabledCheckbox.setChecked(data.enabled);
      }
    }
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
