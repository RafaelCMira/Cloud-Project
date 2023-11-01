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
const DATES_PATH = "../Data/dates.data"

function extractIDsFromFile(filename) {
    try {
    const fileContents = fs.readFileSync(filename, "utf-8");
    const id = extractIDs(fileContents);
    return id;
    } catch (error) {
    console.error("Erro ao ler o ficheiro:", error);
    return [];
    }
}

function extractIDs(line) {
    const matches = line.match(/id='([^']+)'/g); // Encontra todas as ocorrências do atributo 'id'
    if (matches) {
        const ids = matches.map(match => match.match(/'([^']+)'/)[1]); // Extrai os valores do atributo 'id'
        return ids;
    } else
        return [];
}

const usersIds = extractIDsFromFile(USERS_PATH)
const housesIds = extractIDsFromFile(HOUSES_PATH)
const rentalsIds = extractIDsFromFile(RENTALS_PATH)
const datesIds = extractIDsFromFile(DATES_PATH)
var rentals = []

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [
	["/rest/house/rental/", "POST"],
	["rest/house/rental/", "GET"]
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

function loadData() {
	var str;
	if (fs.existsSync(RENTALS_PATH)) {
		str = fs.readFileSync(RENTALS_PATH, "utf8");
		rentals = JSON.parse(str);
	}
}

loadData();

function genNewRental(context, events, done) {
/*    const rentalHouse = housesIds[random(housesIds.length)];
    context.vars.id = rentalHouse + " Rental";
    context.vars.houseId = rentalHouse;
    context.vars.askerId = usersIds[random(usersIds.length)];
    context.vars.price = random(2000);
    const date = datesIds[random(datesIds.length)];
    context.vars.initialDate = date[0]
    context.vars.endDate = date[1]
    context.vars.discount = random(50) */
}

/**
 * Process reply for of new users to store the id on file
 */
function genNewRentalReply(requestParams, response, context, ee, next) {

	if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
		let u = response.body;
		rentals.push(u);
		fs.writeFileSync(HOUSES_PATH, JSON.stringify(rentals));
	} else
	    console.log(response.body)
	return next();
}

