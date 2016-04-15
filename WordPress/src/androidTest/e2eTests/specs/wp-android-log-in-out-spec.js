import assert from 'assert';
import test from 'selenium-webdriver/testing';

import config from 'config';
import * as driverManager from '../lib/driver-manager.js';

import * as driverHelper from '../lib/driver-helper.js';
import webdriver from 'selenium-webdriver';

import LoginFlow from '../lib/flows/login-flow.js';
import LoginPage from '../lib/pages/login-page.js';
import MainPage from '../lib/pages/main-page.js';
import ProfilePage from '../lib/pages/profile-page.js';
import SettingsPage from '../lib/pages/settings-page.js';
import NavbarComponent from '../lib/components/navbar-component.js';

const mochaTimeOut = config.get( 'mochaTimeoutMS' );
const startAppTimeoutMS = config.get( 'startAppTimeoutMS' );

var driver;

test.before( 'Start App', function() {
	this.timeout( startAppTimeoutMS );
	return driverManager.startApp().then( function() {
		driver = global.__APP__;
	} );
} );

test.describe( 'Authentication (' + process.env.ORIENTATION + '):', function() {
	this.timeout( mochaTimeOut );
	test.describe( 'Logging In and Out (.com):', function() {
		test.describe( 'Can Log In', function() {
			test.it( 'Can log in', function() {
				let loginFlow = new LoginFlow( driver );
				loginFlow.login();
			} );

			test.it( 'Can see logged in view after logging in', function() {
				let mainPage = new MainPage( driver );
				mainPage.displayed().then( function( displayed ) {
					assert.equal( displayed, true, 'The main page is not displayed after log in' );
				} );
			} );
		} );

		test.describe( 'Can Log Out', function() {
			test.it( 'Can view profile to log out', function() {
				let navbarComponent = new NavbarComponent( driver );
				navbarComponent.clickProfile();
			} );

			test.it( 'Can logout from profile page', function() {
				let profilePage = new ProfilePage( driver );
				profilePage.disconnectFromWPCom();
			} );

			test.it( 'Can see login page after logging out', function() {
				let loginPage = new LoginPage( driver );
				loginPage.displayed().then( function( displayed ) {
					assert.equal( displayed, true, 'The login page is not displayed after logging out' );
				} );
			} );
		} );
	} );

	test.describe( 'Logging In and Out (.org):', function() {
		test.describe( 'Can Log In', function() {
			test.it( 'Can log in', function() {
				const selfHostedURL = config.get( 'selfHostedURL' );

				let loginFlow = new LoginFlow( driver, 'selfHostedUser', { selfHostedURL: selfHostedURL } );
				loginFlow.login().then( function() {
					assert.equal( 1, 1, 'yes' );
				} );
			} );

			test.it( 'Can see logged in view after logging in', function() {
				let mainPage = new MainPage( driver );
				mainPage.displayed().then( function( displayed ) {
					assert.equal( displayed, true, 'The main page is not displayed after log in' );
				} );
			} );
		} );

		test.describe( 'Can Log Out', function() {
			test.it( 'Can view main site page to log out', function() {
				let navbarComponent = new NavbarComponent( driver );
				navbarComponent.clickSitePanel();
			} );

			test.it( 'Can open Settings', function() {
				let mainPage = new MainPage( driver );
				mainPage.clickSettings();
			} );

			test.it( 'Can logout from settings page', function() {
				let settingsPage = new SettingsPage( driver );
				settingsPage.removeSite();
			} );

			test.it( 'Can see login page after logging out', function() {
				let loginPage = new LoginPage( driver );
				loginPage.displayed().then( function( displayed ) {
					assert.equal( displayed, true, 'The login page is not displayed after logging out' );
				} );
			} );
		} );
	} );
} );
