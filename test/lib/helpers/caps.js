
exports.ios81 = {
  browserName: '',
  'appium-version': '1.3',
  platformName: 'iOS',
  platformVersion: '8.1',
  deviceName: 'iPhone Simulator',
  app: undefined // will be set later
};

exports.android18 = {
  'appium-version': '1.3',
  platformName: 'Android',
  platformVersion: '4.3',
  deviceName: 'Android Emulator',
  'app' : 'sauce-storage:WordPress.apk'
};

exports.android19 = {
  browserName: '',
  'appium-version': '1.3',
  platformName: 'Android',
  platformVersion: '4.4.2',
  deviceName: 'Android Emulator',
  app: undefined // will be set later
};

exports.android22 = {
	"appium-version": "1.4.8",
	platformName: "Android",
	platformVersion: "5.1",
	deviceName: undefined, // will be set later
	app: undefined // will be set later
}

exports.TestObject = {
	'testobject_api_key'         :   process.env.TESTOBJECT_API_KEY,
	'testobject_app_id'          :   '2',
	'testobject_device'          :   'HTC_One_M8_real',
	'testobject_appium_version'  :   '1.3.7',
	'testobject_suite_name'      :   'Suite Life',
	'testobject_test_name'       :   'Build ' + process.env.TRAVIS_BUILD_NUMBER
    };

exports.Nexus7Emulator = {
  'browserName' : '',
  'appiumVersion' : '1.4.14',
  'deviceName' : 'Google Nexus 7 HD Emulator',
  'deviceOrientation' : 'portrait',
  'platformVersion' : '4.4',
  'platformName' : 'Android',
  'app' : 'sauce-storage:WordPress.apk'
};

exports.GalaxyS4Emulator = {
  'browserName' : '',
  'appiumVersion' : '1.4.14',
  'deviceName' : 'Samsung Galaxy S4 Emulator',
  'deviceOrientation' : 'portrait',
  'platformVersion' : '4.4',
  'platformName' : 'Android',
  'app' : 'sauce-storage:WordPress.apk'
};
