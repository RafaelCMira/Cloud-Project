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
const path = require("path");

var imagesIds = [];
var images = [];
var users = [];
var usersIds = [];

// Directory path for data storage
const dataDirectory = path.join(__dirname, "../../Data");

const imagesPath = "../../images/house.";

// Ensure the 'Data' folder exists, create it if it doesn't
if (!fs.existsSync(dataDirectory)) {
	fs.mkdirSync(dataDirectory);
}

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
	return this[random(this.length)];
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
 * Select a user
 */
function selectUser(context, events, done) {
	if (usersIds.length > 0) {
		context.vars.id = usersIds.sample();
	} else {
		delete context.vars.id;
	}
	return done();
}

function genNewUser(context, events, done) {
	const first = `${faker.name.firstName()}`;
	const last = `${faker.name.lastName()}`;
	context.vars.id = first + "." + last;
	context.vars.name = first + " " + last;
	context.vars.pwd = first + "." + last; // `${faker.internet.password()}`;
	return done();
}

/**
 * Process reply for new users to store the id on file
 */
function genNewUserReply(requestParams, response, context, ee, next) {
	if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
		let user = JSON.parse(response.body);
		fs.appendFileSync(path.join(dataDirectory, "users.data"), JSON.stringify(user) + "\n");
	} else
		console.log(response.body);

	return next();
}
