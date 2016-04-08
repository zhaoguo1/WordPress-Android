import test from 'selenium-webdriver/testing';
import config from 'config';
import SlackUpload from 'node-slack-upload';
import fs from 'fs-extra';

import * as mediaHelper from './media-helper';

import * as driverManager from './driver-manager';

const afterHookTimeoutMS = config.get( 'afterHookTimeoutMS' );

test.afterEach( function() {
	this.timeout( afterHookTimeoutMS );

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

			driver.getCurrentUrl().then( ( url ) => console.log( `FAILED: Taking screenshot of: '${url}'` ) );

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
		const prefix = `PASSED-${screenSize}-${shortTestFileName}`;
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

	// Force orientation back to Portrait to prevent issues re-using the emulator for later tests
	return global.__WDDRIVER__.setOrientation( 'PORTRAIT' ).then( function() {
		if ( config.util.getEnv( 'NODE_ENV' ) !== 'development' ) {
			return driverManager.quitApp( global.__APP__ );
		}
	} );
} );
