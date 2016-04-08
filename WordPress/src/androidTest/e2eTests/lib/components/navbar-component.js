import BaseContainer from '../base-container.js';
import { By } from 'selenium-webdriver';
import * as driverHelper from '../driver-helper.js';

export default class NavbarComponent extends BaseContainer {
	constructor( driver ) {
		super( driver, By.id( 'tab_layout' ) );
	}

	clickSitePanel() {
		const sitePanelSelector = By.xpath( '//android.support.v7.app.ActionBar.Tab[1]' );

		return driverHelper.clickWhenClickable( this.driver, sitePanelSelector );
	}

	clickProfile() {
		const profileSelector = By.xpath( '//android.support.v7.app.ActionBar.Tab[3]' );

		return driverHelper.clickWhenClickable( this.driver, profileSelector );
	}
}
