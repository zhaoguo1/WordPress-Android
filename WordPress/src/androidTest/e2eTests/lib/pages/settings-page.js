import BaseContainer from '../base-container.js';
import * as driverHelper from '../driver-helper.js';
import { By } from 'selenium-webdriver';

export default class Settingsage extends BaseContainer {
	constructor( driver ) {
		super( driver, By.id( 'settings' ) );
	}

	removeSite() {
		var driver = this.driver;

		const removeSiteButtonSelector = By.id( 'remove_account' );
		const confirmationSelector = By.id( 'button1' ); //TODO: Get a real ID here

		return driverHelper.clickWhenClickable( driver, removeSiteButtonSelector ).then( function() {
			return driverHelper.clickWhenClickable( driver, confirmationSelector );
		} );
	}
}
