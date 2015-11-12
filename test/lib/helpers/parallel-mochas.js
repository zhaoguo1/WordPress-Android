// Modified code from https://github.com/saucelabs/node-tutorials/blob/master/mocha-wd-parallel/parallel-mochas.js
var exec = require('child_process').exec;
var Q = require('q');
var _ = require('lodash');

var mochaArgs = process.argv[2];

// building device list
var devices = process.argv.splice(3);

// context specific log
function log(config, data) {
  config = (config + '          ').slice(0, 10);
  _(('' + data).split(/(\r?\n)/g)).each(function(line) {        
    if(line.replace(/\033\[[0-9;]*m/g,'').trim().length >0) { 
      console.log(config + ': ' + line.trimRight() );
    }
  });
}

// runs the mocha tests for a given device
function run_mocha(device, done) {
  var env = _(process.env).clone();
  env.DEVICE = device;

  var mocha = exec('mocha ' + mochaArgs + ' #' + device, {env: env}, done);
  mocha.stdout.on('data', log.bind(null, device));
  mocha.stderr.on('data', log.bind(null, device));
}

// building job list
var jobs = _(devices).map(function(device) {
  return Q.nfcall(run_mocha, device);
}).value();

// running jobs in parallel
Q.all(jobs).done(function onFulfilled(array) {
    console.log("ALL TESTS PASSED!");
}, function onRejected(reject) {
    var device = "at least one device";
    var msgSplit = reject.message.split('#');
    if (msgSplit.length > 0) {
      device = msgSplit[msgSplit.length];
console.log(msgSplit);
    }
    throw(new Error("Test for " + device + " failed.  Other devices may have also failed, check Test Object/Sauce Labs for details"));
});
