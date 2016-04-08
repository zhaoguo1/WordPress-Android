import BaseContainer from '../base-container.js';
import * as driverHelper from '../driver-helper.js';
import { By } from 'selenium-webdriver';

export default class MainPage extends BaseContainer {
	constructor( driver ) {
		super( driver, By.id( 'viewpager_main' ) );
	}

	clickSettings() {
		const settingsSelector = By.id( 'my_site_settings_text_view' );

		return driverHelper.clickWhenClickable( this.driver, settingsSelector );
	}
}
