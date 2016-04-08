if (process.env.SAUCE) {
  exports.WordPressAPK = "sauce-storage:WordPress.apk";
} else if (process.env.TESTOBJECT) {
  exports.WordPressAPK = undefined;
}else {
  exports.WordPressAPK = "./WordPress-vanilla-debug.apk";
}
