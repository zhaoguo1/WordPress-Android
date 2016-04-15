import wd from 'wd';
import _ from 'lodash';
//import config from 'config';
import webdriver from 'selenium-webdriver';
var wdBridge = require( 'wd-bridge' )( webdriver, wd );

//const webDriverImplicitTimeOutMS = 60000;
import deviceConfig from '../lib/helpers/capabilities';

let driver, wdDriver, originalScreenSize;

export function startApp() {
	if ( global.__APP__ ) {
		return global.__APP__;
	}

	//TODO: device name?	const screenSize = this.currentScreenSize();

	let caps = new webdriver.Capabilities();

	let device = deviceConfig[ process.env.DEVICE ];
	_.each( device, function( val, key ) {
		caps.set( key, val );
	} );

	let builder;
	if ( process.env.SAUCE ) {
		caps.set( 'username', 'wordpress-android' );
		caps.set( 'accessKey', process.env.SAUCE_TOKEN );
		caps.set( 'name', `WordPress Android - ${process.env.ORIENTATION} - #${process.env.CIRCLE_BUILD_NUM}` );
		caps.set( 'tags', ['sample'] );
		builder = new webdriver.Builder()
			.usingServer( 'http://ondemand.saucelabs.com:80/wd/hub' )
			.withCapabilities( caps );
	} else {
		caps.set( 'app', './WordPress/build/outputs/apk/WordPress-wasabi-debug.apk' );
		builder = new webdriver.Builder()
			.usingServer( 'http://localhost:4723/wd/hub' )
			.withCapabilities( caps );
	}

	global.__APP__ = driver = builder.build();
	return wdBridge.initFromSeleniumWebdriver( builder, driver )
		.then( function( _wdDriver ) {
			global.__WDDRIVER__ = wdDriver = _wdDriver;
			wdDriver.getWindowSize().then( function( _screenSize ) {
				originalScreenSize = _screenSize.height + 'x' + _screenSize.width;
			} );
			return wdDriver.setOrientation( process.env.ORIENTATION || 'PORTRAIT' );
		} );
}

export function getOriginalScreenSize() {
	return originalScreenSize;
}

export function quitApp( _driver ) {
	_driver.quit();
}
