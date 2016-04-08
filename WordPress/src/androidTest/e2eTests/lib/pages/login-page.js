import BaseContainer from '../base-container.js';
import { By } from 'selenium-webdriver';
import * as driverHelper from '../driver-helper.js';

export default class LoginPage extends BaseContainer {
	constructor( driver ) {
		super( driver, By.id( 'nux_fragment_icon' ) );

		this.userNameSelector = By.id( 'nux_username' );
		this.passwordSelector = By.id( 'nux_password' );
		this.submitSelector = By.id( 'nux_sign_in_button' );
	}

	login( username, password ) {
		var driver = this.driver;
		const userNameSelector = this.userNameSelector;

		driverHelper.setWhenSettable( driver, this.userNameSelector, username );
		driverHelper.setWhenSettable( driver, this.passwordSelector, password );

		driverHelper.clickWhenClickable( driver, this.submitSelector );

		return driver.wait( function() {
			return driver.isElementPresent( userNameSelector ).then( function( present ) {
				return !present;
			} );
		}, this.explicitWaitMS, 'The login form is still displayed after submitting the logon form' );
	}

	addSelfHostedURL( url ) {
		var driver = this.driver;
		const selfHostedButtonSelector = By.id( 'nux_add_selfhosted_button' );
		const selfHostedURLSelector = By.id( 'nux_url' );

		return driverHelper.clickWhenClickable( driver, selfHostedButtonSelector ).then( function() {
			driverHelper.setWhenSettable( driver, selfHostedURLSelector, url );
		} );
	}
}
