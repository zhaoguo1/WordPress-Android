import BaseContainer from '../base-container.js';
import * as driverHelper from '../driver-helper.js';

import { By } from 'selenium-webdriver';

export default class ProfilePage extends BaseContainer {
	constructor( driver ) {
		super( driver, By.id( 'frame_avatar' ) );
	}

	disconnectFromWPCom() {
		var driver = this.driver;
		const disconnectButtonSelector = By.id( 'row_logout' );
		const disconnectConfirmationSelector = By.id( 'button1' ); //TODO: Get a real ID here

		return driverHelper.clickWhenClickable( driver, disconnectButtonSelector ).then( function() {
			return driverHelper.clickWhenClickable( driver, disconnectConfirmationSelector );
		} );
	}
}
