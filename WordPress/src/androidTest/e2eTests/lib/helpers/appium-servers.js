
exports.local = {
  host: 'localhost',
  port: 4723
};

exports.testObject = {
  host : 'app.testobject.com:443',
  pathname : '/api/appium/wd/hub',
  protocol : 'https'
};

exports.sauce = {
  host: 'ondemand.saucelabs.com',
  port: 80,
  username: process.env.SAUCE_USERNAME,
  password: process.env.SAUCE_ACCESS_KEY
};
