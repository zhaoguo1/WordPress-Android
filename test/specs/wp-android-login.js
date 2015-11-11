"use strict";

require("../helpers/setup");

var wd = require("wd"),
    _ = require('underscore'),
    serverConfigs = require('../helpers/appium-servers');

describe("WordPress Android", function () {
  this.timeout(300000);
  var driver;
  var allPassed = true;

  before(function () {
    var serverConfig =  serverConfigs.local;

    if (process.env.SAUCE)   { serverConfig = serverConfigs.sauce; }
    else if (process.env.TESTOBJECT) { serverConfig = serverConfigs.testObject; }

    driver = wd.promiseChainRemote(serverConfig);
    require("../helpers/logging").configure(driver);

    var desired = _.clone(require("../helpers/caps").android18); 

    if (process.env.SAUCE)   { desired = _.clone(require("../helpers/caps").android18); }
    else if (process.env.TESTOBJECT) { desired =  _.clone(require("../helpers/caps").TestObject); }

    if (process.env.SAUCE) {
      desired.app = 'sauce-storage:WordPress.apk';
      desired.name = 'WordPress - Login';
      desired.tags = ['travisci'];
    } else if (process.env.TESTOBJECT) {
      desired.app = undefined;
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
        .elementById("org.wordpress.android:id/nux_username").click().sendKeys("hd83")
        .elementById("org.wordpress.android:id/nux_password").click().sendKeys("Abcde123@")
	.takeScreenshot()
        .elementById("org.wordpress.android:id/nux_sign_in_button").click()
        .elementByXPath(loggedInConfirmation).should.eventually.exist
	.takeScreenshot();
  });
});

