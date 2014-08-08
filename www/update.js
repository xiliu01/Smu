
var exec = require('cordova/exec');

var Update = {};

Update.update = function(successCallback, errorCallback,url) {
    exec(successCallback, errorCallback, "Update", "update", [url]);
};

module.exports = Update;