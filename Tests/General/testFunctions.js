"use strict";

/**
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
	uploadImageBody,
	genNewUser,
	genNewUserReply,
	genNewHouse,
    genNewHouseReply,
    genNewQuestion,
    genNewQuestionReply,
    genNewRental,
    genNewRentalReply
};

const faker = require("faker");
const fs = require("fs");
const path = require("path");

// Directory path for data storage
const DATA_FOLDER = path.join(__dirname, "../Data");

const USERS2_PATH = "users2.data";

const IMAGES_PATH = "../images/house.";
const USERS_PATH = "../Data/users.data";
const HOUSES_PATH = "../Data/houses.data";
const QUESTIONS_PATH = "../Data/questions.data";
const RENTALS_PATH = "../Data/rentals.data";

var imagesIds = []
var images = [];
var users = [];
var houses = [];
var questions = [];
var rentals = [];

// Ensure the 'Data' folder exists, create it if it doesn't
if (!fs.existsSync(DATA_FOLDER)) {
	fs.mkdirSync(DATA_FOLDER);
}

Array.prototype.sample = function () { return this[random(this.length)];};

function random(val) { return Math.floor(Math.random() * val); }

// Loads data about images from disk
function loadData() {
    var i;
    for (i = 1; i <= 40; i++) {
        var img = fs.readFileSync(IMAGES_PATH + i + ".jpg");
        images.push(img);
    }
  /*  var str;
    if( fs.existsSync(USERS_PATH)) {
        str = fs.readFileSync(USERS2_PATH,'utf8')
        users = JSON.parse(str)
    } */
}

loadData();

// ######################     IMAGES       ###########################

function uploadImageBody(requestParams, context, ee, next) {
	requestParams.body = images.sample();
	return next();
}

function processUploadReply(requestParams, response, context, ee, next) {
	if (typeof response.body !== "undefined" && response.body.length > 0) {
		imagesIds.push(response.body);
	}
	return next();
}

function selectImageToDownload(context, events, done) {
	if (imagesIds.length > 0) {
		context.vars.imageId = imagesIds.sample();
	} else {
		delete context.vars.imageId;
	}
	return done();
}


// ######################     USERS       ###########################

function genNewUser(context, events, done) {
	const first = `${faker.name.firstName()}`;
	const last = `${faker.name.lastName()}`;
	context.vars.id = first + "." + last;
	context.vars.name = first + " " + last;
	context.vars.pwd = first + "." + last; // `${faker.internet.password()}`;
	return done();
}

function genNewUserReply(requestParams, response, context, ee, next) {
	if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
		let user = JSON.parse(response.body);
		users.push(user)
		fs.appendFileSync(USERS_PATH, JSON.stringify(user) + "\n");
	} else
		console.log(response.body);

	return next();
}

// ######################     HOUSES       ###########################

function randomInBetween(min, max) {
    let i = Math.random();
    if (i > 0.85) {
        return min + Math.floor(Math.random() * max);
    }
    return 0;
}

const cities = ["Lisboa", "Porto", "Almada", "Cova da moura", "Chicago", "Houston", "Miami", "Toronto", "Barcelona", "Berlin", "Budapest"];

function genNewHouse(context, events, done) {
    const houseName = `${faker.address.streetName()}`;
    context.vars.id = houseName;
    context.vars.name = houseName;
    context.vars.location = cities.sample();
    context.vars.description = faker.lorem.sentence();
    context.vars.ownerId = users.sample().id;
    console.log("onwerId = " + context.vars.ownerId);
    context.vars.price = random(300);
    context.vars.discount = randomInBetween(context.vars.price * 0.05, context.vars.price / 2);
    return done();
}

function genNewHouseReply(requestParams, response, context, ee, next) {
    if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
        let house = JSON.parse(response.body);
        houses.push(house)
        fs.appendFileSync(HOUSES_PATH, JSON.stringify(house) + "\n");
    } else
        console.log(response.body);

    return next();
}

// ######################     QUESTIONS       ###########################

function genNewQuestion(context, events, done) {
    context.vars.houseId = houses.sample().id;
    context.vars.askerId = users.sample().id;
    context.vars.text = generateRandomQuestion();
	return done();
}

function generateRandomQuestion() {
  const questionTypes = ['What', 'When', 'Where', 'Who', 'Why', 'How'];
  const randomQuestionType = faker.random.arrayElement(questionTypes);
  const randomSubject = faker.random.words();
  const randomAction = faker.random.words();

  const question = `${randomQuestionType} is ${randomSubject} ${randomAction}?`;
  return question;
}

function genNewQuestionReply(requestParams, response, context, ee, next) {
	if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
		let question = JSON.parse(response.body);
		questions.push(question);
		fs.writeFileSync(QUESTIONS_PATH, JSON.stringify(question) + "\n");
	} else
	    console.log(response.body)

	return next();
}

// ######################     RENTALS       ###########################

const formatDate = (date) => {
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${year}-${month}-${day}`;
};

function genNewRental(context, events, done) {
    const initialDate = new Date();
    const endDate = faker.date.between(initialDate, new Date('2024-12-31'));

    context.vars.houseId = houses.sample().id;
    context.vars.userId = users.sample().id;
    context.vars.initialDate = formatDate(initialDate);
    context.vars.endDate = formatDate(endDate);
    return done();
}

function genNewRentalReply(requestParams, response, context, ee, next) {
	if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
		let rental = JSON.parse(response.body);
		rentals.push(rental);
		fs.appendFileSync(RENTALS_PATH, JSON.stringify(rental)  + "\n");
	} else
	    console.log(response.body)

	return next();
}