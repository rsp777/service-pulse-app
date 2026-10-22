import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';
import { ServiceModalComponent } from '../components/ServiceModalComponent';

/**
 * ServicesPage — Service Registry view for browsing and configuring services.
 */
export class ServicesPage extends BasePage {
  readonly heading: Locator;
  readonly addServiceButton: Locator;
  readonly filterServerSelect: Locator;
  readonly servicesTable: Locator;
  readonly serviceModal: ServiceModalComponent;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: 'Service Registry', level: 2 });
    this.addServiceButton   = page.getByRole('button', { name: '+ Add service' }).first();
    this.filterServerSelect = page.locator('#service-filter-server');
    this.servicesTable      = page.locator('#all-services-table');
    this.serviceModal       = new ServiceModalComponent(page);
  }

  async filterByServer(serverName: string) {
    await this.filterServerSelect.selectOption({ label: serverName });
  }

  async openAddServiceModal() {
    await this.addServiceButton.click();
  }
}
