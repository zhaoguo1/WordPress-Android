import wd from 'wd';
import _ from 'lodash';
//import config from 'config';
import webdriver from 'selenium-webdriver';
var wdBridge = require( 'wd-bridge' )( webdriver, wd );

//const webDriverImplicitTimeOutMS = 60000;
import deviceConfig from '../lib/helpers/devices';

let driver, wdDriver, originalScreenSize;

export function startApp() {
	if ( global.__APP__ ) {
		return global.__APP__;
	}

	//TODO: device name?	const screenSize = this.currentScreenSize();

	let caps = new webdriver.Capabilities();

	let device = deviceConfig[ process.env.DEVICE || 'android22' ];
	_.each( device, function( val, key ) {
		caps.set( key, val );
	} );

//	caps.set( 'app', '../WordPress/build/outputs/apk/WordPress-wasabi-debug.apk' );
	caps.set( 'app', './WordPress/build/outputs/apk/WordPress-vanilla-debug.apk' );

	let builder = new webdriver.Builder()
		.usingServer( 'http://localhost:4723/wd/hub' )
		.withCapabilities( caps );

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
