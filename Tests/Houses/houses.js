"use strict";


// Exported functions to be used in the testing scripts.
module.exports = {
    uploadImageBody,
    genNewHouse,
    genNewHouseReply,
};

const faker = require("faker");
const fs = require("fs");

const HOUSES_PATH = "../Data/houses.data";
const USER_PATH = "../Data/users.data";

function extractUserIdsFromFile(filename) {
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

const usersIds = extractUserIdsFromFile(USER_PATH);
var images = [];
var houses = [];

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [
    ["/rest/media", "POST"],
    ["/rest/house/", "POST"],
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
    if (fs.existsSync("/..images")) basefile = "/..images/house.";
    else basefile = "../images/house.";
    for (i = 1; i <= 40; i++) {
        var img = fs.readFileSync(basefile + i + ".jpg");
        images.push(img);
    }
}

loadData();

/**
 * Sets the body to an image when using images.
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
        images.push(response.body);
    }
    return next();
}

/**
 * Select an image to download.
 */
function selectImageToDownload(context, events, done) {
    if (images.length > 0) {
        context.vars.imageId = images.sample();
    } else {
        delete context.vars.imageId;
    }
    return done();
}


function genNewHouse(context, events, done) {
    const houseName = `${faker.address.streetName()}`;
    context.vars.id = houseName;
    context.vars.name = houseName;
    context.vars.location = faker.address.city();
    context.vars.description = faker.lorem.sentence();
    context.vars.ownerId = usersIds.sample();
    console.log("onwerId = " + context.vars.ownerId);
    context.vars.price = random(300);
    return done();
}

/**
 * Process reply for new houses to store the data in the file
 */
function genNewHouseReply(requestParams, response, context, ee, next) {
    if (response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0) {
        let house = JSON.parse(response.body);
        fs.appendFileSync(HOUSES_PATH, JSON.stringify(house) + "\n"); // Write the entire 'houses' array to the file
    } else
        console.log(response.body);

    return next();
}
