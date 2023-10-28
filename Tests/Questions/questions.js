"use strict";

module.exports = {
	genNewQuestion,
	genNewQuestionReply,
};

const faker = require("faker");
const fs = require("fs");

const HOUSES_PATH = "../Data/houses.data";
const USERS_PATH = "../Data/users.data";
const QUESTIONS_PATH = "../Data/questions.data";

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
var questions = [];

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [
	["/rest/question/", "POST"],
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
	if (fs.existsSync(QUESTIONS_PATH)) {
		str = fs.readFileSync(HOUSES_PATH, "utf8");
		questions = JSON.parse(str);
	}
}

loadData();

function genNewQuestion(context, events, done) {
    context.vars.houseId = housesIds[random(housesIds.length)];
    context.vars.askerId = usersIds[random(usersIds.length)];
    context.vars.text = generateRandomQuestion();
	return done();
}

// Create a function to generate a random question
function generateRandomQuestion() {
  const questionTypes = ['What', 'When', 'Where', 'Who', 'Why', 'How'];
  const randomQuestionType = faker.random.arrayElement(questionTypes);
  const randomSubject = faker.random.words();
  const randomAction = faker.random.words();

  const question = `${randomQuestionType} is ${randomSubject} ${randomAction}?`;
  return question;
}

/**
 * Process reply for of new users to store the id on file
 */
function genNewQuestionReply(requestParams, response, context, ee, next) {
	if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
		let u = response.body;
		questions.push(u);
		fs.writeFileSync(QUESTIONS_PATH, JSON.stringify(questions));
	} else
	    console.log(response.body)
	return next();
}

