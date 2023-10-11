const faker = require('faker');
/*
// Generate a random name
const randomName = faker.name.firstName();
console.log("Random Name:", randomName);

// Generate a random email address
const randomEmail = faker.internet.email();
console.log("Random Email:", randomEmail);

// Generate a random address
const randomAddress = faker.address.streetAddress();
console.log("Random Address:", randomAddress);

// Generate a random phone number
const randomPhone = faker.phone.phoneNumber();
console.log("Random Phone Number:", randomPhone);*/


const first = `${faker.name.firstName()}`
console.log(first);
	const last = `${faker.name.lastName()}`
console.log(last);

	pwd = `${faker.internet.password()}`

console.log(pwd);

