"use strict"

/**
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
    genNewRental,
    genNewRentalReply
};

const faker = require("faker");
const fs = require("fs");

const HOUSES_PATH = "../Data/houses.data";
const USERS_PATH = "../Data/users.data";
const RENTALS_PATH = "../Data/rentals.data";

function extractIdsFromFile(filename) {
    try {
        const fileContents = fs.readFileSync(filename, "utf-8");
        const lines = fileContents.split('\n').filter(Boolean); // Split into lines and remove empty lines
        const ids = lines.map(line => {
            const data = JSON.parse(line);
            return data.id;
        });
        return ids;
    } catch (error) {
        console.error("Error reading the file:", error);
        return [];
    }
}

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

const usersIds = extractIdsFromFile(USERS_PATH)
const housesIds = extractIdsFromFile(HOUSES_PATH)
var rentals = []

var houseSelected = housesIds.sample();

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [
	["/rest/house/" + houseSelected + "/rental/", "POST"],
];

function formatDate(date) {
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
}

// Generate a random initial date and end date


function genNewRental(context, events, done) {
    const initialDate = faker.date.past();
    const endDate = faker.date.between(initialDate, new Date());
    const formattedInitialDate = formatDate(initialDate);
    const formattedEndDate = formatDate(endDate);

    context.vars.houseId = houseSelected;
    context.vars.userId = usersIds.sample();
    context.vars.initialDate = formattedInitialDate
    context.vars.endDate = formattedEndDate
    return done();
}

/**
 * Process reply for of new users to store the id on file
 */
function genNewRentalReply(requestParams, response, context, ee, next) {

	if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
		let rental = JSON.parse(response.body);
		fs.appendFileSync(RENTALS_PATH, JSON.stringify(rental));
	} else
	    console.log(response.body)

	return next();
}

