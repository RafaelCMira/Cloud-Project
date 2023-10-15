"use strict";

/**
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
	uploadImageBody,
	genNewUser,
	genNewUserReply,
};

const faker = require("faker");
const fs = require("fs");

var imagesIds = [];
var images = [];
var users = [];

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [
	["/rest/media/", "GET"],
	["/rest/media", "POST"],
	["/rest/user/", "GET"],
	["/rest/user/", "POST"],
];

// Function used to compress statistics
global.myProcessEndpoint = function (str, method) {
	var i = 0;
	for (i = 0; i < statsPrefix.length; i++) {
		if (str.startsWith(statsPrefix[i][0]) && method == statsPrefix[i][1]) return method + ":" + statsPrefix[i][0];
	}
	return method + ":" + str;
};

// Auxiliary function to select an element from an array
Array.prototype.sample = function () {
	return this[Math.floor(Math.random() * this.length)];
};

// Returns a random value, from 0 to val
function random(val) {
	return Math.floor(Math.random() * val);
}

// Loads data about images from disk
function loadData() {
	var i;
	var basefile;
	if (fs.existsSync("/../../images")) basefile = "/../../images/house.";
	else basefile = "../../images/house.";
	for (i = 1; i <= 40; i++) {
		var img = fs.readFileSync(basefile + i + ".jpg");
		images.push(img);
	}
	var str;
	if (fs.existsSync("users.data")) {
		str = fs.readFileSync("users.data", "utf8");
		users = JSON.parse(str);
	}
}

loadData();

/**
 * Sets the body to an image, when using images.
 */
function uploadImageBody(requestParams, context, ee, next) {
	requestParams.body = images.sample();
	return next();
}

/**
 * Process reply of the download of an image.
 * Update the next image to read.
 */
function processUploadReply(requestParams, response, context, ee, next) {
	if (typeof response.body !== "undefined" && response.body.length > 0) {
		imagesIds.push(response.body);
	}
	return next();
}

/**
 * Select an image to download.
 */
function selectImageToDownload(context, events, done) {
	if (imagesIds.length > 0) {
		context.vars.imageId = imagesIds.sample();
	} else {
		delete context.vars.imageId;
	}
	return done();
}

/**
 * Select an image to download.
 */
function selectUser(context, events, done) {
	if (userIDs.length > 0) {
		context.vars.userID = userIDs.sample();
	} else {
		delete context.vars.userID;
	}
	return done();
}


function genNewUser(context, events, done) {
	const first = `${faker.name.firstName()}`;
	const last = `${faker.name.lastName()}`;
	context.vars.userID = first + "." + last;
	context.vars.name = first + " " + last;
	context.vars.pwd = `${faker.internet.password()}`;
	return done();
}

/**
 * Process reply for of new users to store the id on file
 */
function genNewUserReply(requestParams, response, context, ee, next) {
	if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
		// console.log(response.body)
		let u = response.body;
		users.push(u);
		fs.writeFileSync("users.data", JSON.stringify(users));
	}
	return next();
}
