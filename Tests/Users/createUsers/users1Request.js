"use strict";

/**
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
    selectRandomImage,
	genNewUser,
	genNewUserReply,
};

const faker = require("faker");
const fs = require("fs");

var images = [];
var users = [];

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [
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


// Returns a random value, from 0 to val
function random(val) {
    return Math.floor(Math.random() * val);
}

// Auxiliary function to select an element from an array
Array.prototype.sample = function () {
	return this[random(this.length)];
};


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


function selectRandomImage(userContext, events, done) {
    userContext.vars.image = images.sample();
    return done();
}


function genNewUser(context, events, done) {
	const first = `${faker.name.firstName()}`;
	const last = `${faker.name.lastName()}`;
	context.vars.id = first + "." + last;
	context.vars.name = first + " " + last;
	context.vars.pwd = `${faker.internet.password()}`;
	context.vars.image = images.sample();
	return done();
}

/**
 * Process reply for of new users to store the id on file
 */
function genNewUserReply(requestParams, response, context, ee, next) {
	if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
		let u = response.body;
		users.push(u);
		fs.writeFileSync("users.data", JSON.stringify(users));
	} else
	    console.log(response.body)

	return next();
}
