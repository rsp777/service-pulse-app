import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';
import { AlertModalComponent } from '../components/AlertModalComponent';
import { LogEventModalComponent } from '../components/LogEventModalComponent';
import { MappingModalComponent } from '../components/MappingModalComponent';

/**
 * AlertsPage — Alert Rules, Triggered Events Log, and Condition Types & Mappings view.
 */
export class AlertsPage extends BasePage {
  readonly heading: Locator;
  readonly refreshButton: Locator;
  readonly newAlertButton: Locator;

  // Tabs
  readonly rulesTabButton: Locator;
  readonly eventsTabButton: Locator;
  readonly conditionsTabButton: Locator;

  // Panels
  readonly rulesContainer: Locator;
  readonly eventsContainer: Locator;
  readonly conditionsContainer: Locator;
  readonly openLogEventButton: Locator;
  readonly addConditionMappingButton: Locator;

  // Modals
  readonly alertModal: AlertModalComponent;
  readonly logEventModal: LogEventModalComponent;
  readonly mappingModal: MappingModalComponent;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: /Alert & Condition Management/i, level: 2 });
    this.refreshButton = page.locator('#refresh-alerts');
    this.newAlertButton = page.locator('#add-alert');

    this.rulesTabButton = page.locator('button[data-alert-tab="rules"]');
    this.eventsTabButton = page.locator('button[data-alert-tab="events"]');
    this.conditionsTabButton = page.locator('button[data-alert-tab="conditions"]');

    this.rulesContainer = page.locator('#alert-tab-rules');
    this.eventsContainer = page.locator('#alert-tab-events');
    this.conditionsContainer = page.locator('#alert-tab-conditions');
    this.openLogEventButton = page.locator('#open-log-event-modal');
    this.addConditionMappingButton = page.locator('#add-condition-mapping-btn');

    this.alertModal = new AlertModalComponent(page);
    this.logEventModal = new LogEventModalComponent(page);
    this.mappingModal = new MappingModalComponent(page);
  }

  async switchToRulesTab() {
    await this.rulesTabButton.click();
  }

  async switchToEventsTab() {
    await this.eventsTabButton.click();
  }

  async switchToConditionsTab() {
    await this.conditionsTabButton.click();
  }

  async openAddAlertModal() {
    await this.newAlertButton.click();
  }

  async openLogEventModal() {
    await this.switchToEventsTab();
    await this.openLogEventButton.click();
  }
}
