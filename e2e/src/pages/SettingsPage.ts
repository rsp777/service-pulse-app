import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';
import { PathModalComponent } from '../components/PathModalComponent';
import { ScriptModalComponent } from '../components/ScriptModalComponent';

/**
 * SettingsPage — Runtime Configuration, Configured Paths, and Configured Scripts view.
 */
export class SettingsPage extends BasePage {
  readonly heading: Locator;
  readonly runtimeConfigStatus: Locator;

  // Runtime Config checkboxes
  readonly healthcheckCheckbox: Locator;
  readonly serviceManagementCheckbox: Locator;
  readonly alertManagementCheckbox: Locator;
  readonly healthStatusPopulationCheckbox: Locator;
  readonly healthcheckCronInput: Locator;
  readonly saveConfigButton: Locator;

  // Paths
  readonly pathsHeading: Locator;
  readonly addPathButton: Locator;
  readonly pathTable: Locator;
  readonly pathModal: PathModalComponent;

  // Scripts
  readonly scriptsHeading: Locator;
  readonly addScriptButton: Locator;
  readonly scriptTable: Locator;
  readonly scriptModal: ScriptModalComponent;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: 'Runtime Configuration', level: 2 });
    this.runtimeConfigStatus = page.locator('#runtime-config-status');

    this.healthcheckCheckbox = page.locator('#healthcheck-enabled');
    this.serviceManagementCheckbox = page.locator('#service-management-enabled');
    this.alertManagementCheckbox = page.locator('#alert-management-enabled');
    this.healthStatusPopulationCheckbox = page.locator('#health-status-population-enabled');
    this.healthcheckCronInput = page.locator('#healthcheck-cron');
    this.saveConfigButton = page.locator('#save-runtime-config');

    this.pathsHeading = page.getByRole('heading', { name: 'Configured Paths', level: 2 });
    this.addPathButton = page.locator('#add-path');
    this.pathTable = page.locator('#path-table');
    this.pathModal = new PathModalComponent(page);

    this.scriptsHeading = page.getByRole('heading', { name: 'Configured Scripts', level: 2 });
    this.addScriptButton = page.locator('#add-script');
    this.scriptTable = page.locator('#script-table');
    this.scriptModal = new ScriptModalComponent(page);
  }

  async openAddPathModal() {
    await this.addPathButton.click();
  }

  async openAddScriptModal() {
    await this.addScriptButton.click();
  }
}
