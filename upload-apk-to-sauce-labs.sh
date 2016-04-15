#!/bin/bash
APK_PATH=WordPress/build/outputs/apk
APK=$1

curl -u wordpress-android:$SAUCE_TOKEN -X POST -H "Content-Type: application/octet-stream" https://saucelabs.com/rest/v1/storage/wordpress-android/$APK\?overwrite=true --data-binary @$APK_PATH/$APK
