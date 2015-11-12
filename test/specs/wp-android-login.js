"use strict";

require("../lib/helpers/setup");

var wd = require("wd"),
    _ = require('underscore'),
    serverConfigs = require('../lib/helpers/appium-servers'),
    DESIREDS = require('../lib/helpers/caps');

var test_username = process.env.TEST_USERNAME;
var test_password = process.env.TEST_PASSWORD;
var deviceKey = process.env.DEVICE || 'android18';
var desired = DESIREDS[deviceKey];

describe("WordPress Android - " + desired.deviceName, function () {
  this.timeout(300000);
  var driver;
  var allPassed = true;

  before(function () {
    var serverConfig = serverConfigs.sauce;

    driver = wd.promiseChainRemote(serverConfig);

    if (process.env.APPIUM_DEBUG) {
      require("../lib/helpers/logging").configure(driver);
    }

    desired.name = 'WordPress Android - Login';
    desired.tags = [deviceKey];

// Induce failure for a single device
if (deviceKey === 'android18') {
  test_password = "fff";
}

    return driver
      .init(desired)
      .setImplicitWaitTimeout(360000);
  });

  after(function () {
    return driver
      .quit()
      .finally(function () {
        if (process.env.SAUCE) {
          return driver.sauceJobStatus(allPassed);
        }
      });
  });

  afterEach(function () {
    allPassed = allPassed && this.currentTest.state === 'passed';
  });

  it("should log in", function () {
    var loggedInConfirmation = "//android.widget.LinearLayout[1]/android.widget.FrameLayout[1]/android.widget.RelativeLayout[1]/android.widget.HorizontalScrollView[1]/android.widget.LinearLayout[1]/android.support.v7.app.ActionBar.Tab[1]";
    return driver.elementById("org.wordpress.android:id/nux_username")
        .should.eventually.exist
        .elementById("org.wordpress.android:id/nux_username").click().sendKeys(test_username)
        .elementById("org.wordpress.android:id/nux_password").click().sendKeys(test_password)
	.takeScreenshot()
        .elementById("org.wordpress.android:id/nux_sign_in_button").click()
        .elementByXPath(loggedInConfirmation).should.eventually.exist
	.takeScreenshot();
  });
});

//var request = require('request')
//var username = process.env.TESTOBJECT_API_KEY;
//var password = "";
//var url = 'https://app.testobject.com:443/api/rest/appium/v1/suites/7/devices'
//var auth = "Basic " + new Buffer(username + ":" + password).toString("base64");
//request({ url : url, headers : { "Authorization" : auth } }, function(e, r, b) {
//console.log(r);
//});
