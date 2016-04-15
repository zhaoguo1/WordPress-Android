import test from 'selenium-webdriver/testing';
import config from 'config';
import SlackUpload from 'node-slack-upload';
import fs from 'fs-extra';

import * as mediaHelper from './media-helper';

import * as driverManager from './driver-manager';

const afterHookTimeoutMS = config.get( 'afterHookTimeoutMS' );
var allPassed = true;

test.afterEach( function() {
	this.timeout( afterHookTimeoutMS );
	allPassed = allPassed && this.currentTest.state === 'passed';

	const driver = global.__APP__;
	const longTestName = this.currentTest.fullTitle();
	const shortTestFileName = this.currentTest.title.replace( /[^a-z0-9]/gi, '-' ).toLowerCase();
	const screenSize = driverManager.getOriginalScreenSize();
	const orientation = process.env.ORIENTATION || 'PORTRAIT';

	if ( this.currentTest.state === 'failed' ) {
		const prefix = `FAILED-${screenSize}-${orientation}-${shortTestFileName}`;
		try {
			let neverSaveScreenshots = config.get( 'neverSaveScreenshots' );
			if ( neverSaveScreenshots ) {
				return null;
			}

			console.log( `FAILED: Taking screenshot` );

			return driver.takeScreenshot().then( ( data ) => {
				let screenshotPath = mediaHelper.writeScreenshot( data, prefix );

				if ( process.env.SLACK_TOKEN && process.env.CIRCLE_BRANCH === 'master' ) {
					let slackUpload = new SlackUpload( process.env.SLACK_TOKEN );

					slackUpload.uploadFile( {
						file: fs.createReadStream( screenshotPath ),
						title: `${longTestName} - # ${process.env.CIRCLE_BUILD_NUM}`,
						channels: '#e2eflowtesting-notif'
					}, function( err ) {
						if ( err ) {
							console.error( 'Upload failed: ' + err );
						} else {
							console.log( 'done' );
						}
					} );
				}
			} );
		} catch ( e ) {
			console.log( `Error when taking screenshot in base container: '${e}'` );
		}
	}
	if ( config.get( 'saveAllScreenshots' ) === true ) {
		const prefix = `PASSED-${screenSize}-${orientation}-${shortTestFileName}`;
		try {
			return driver.takeScreenshot().then( ( data ) => {
				mediaHelper.writeScreenshot( data, prefix );
			} );
		} catch ( e ) {
			console.log( `Error when taking screenshot in base container: '${e}'` );
		}
	}
} );

test.after( function() {
	this.timeout( afterHookTimeoutMS );
	const driver = global.__APP__;
	const wdDriver = global.__WDDRIVER__;

	if ( process.env.SAUCE ) {
		return wdDriver.sauceJobStatus( allPassed );
	}

	// For non-SauceLabs runs, force orientation back to Portrait to prevent issues re-using the emulator for later tests
	return wdDriver.setOrientation( 'PORTRAIT' ).then( function() {
		if ( config.util.getEnv( 'NODE_ENV' ) !== 'development' ) {
			return driverManager.quitApp( driver );
		}
	} );
} );
