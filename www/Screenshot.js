var exec = require('cordova/exec');

module.exports = {
	takeScreenshot: function(callback, quality, scaleDown) {
		quality = quality || 100;
		scaleDown = scaleDown || 1;
		exec(function(res){
			callback && callback(null, res);
		}, function(error){
			callback && callback(error);
		}, "Screenshot", "takeScreenshot", [quality, scaleDown]);
	}
};
