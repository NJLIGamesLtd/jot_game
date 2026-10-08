'use strict';

const functions = require('firebase-functions');
const sanitizer = require('./sanitizer');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);



// Return a t_user entry based on its ID.
exports.getUser = functions.https.onCall((data) =>
{
	const idUser = data.idUser;

	// Confirm we have a valid ID.
	if (!Number.isFinite(idUser) ||
		idUser <= 0)
	{
		// Throwing an HttpsError so that the client gets the error details.
		throw new functions.https.HttpsError('invalid-argument', 'Need a valid user ID.');
	}

	return admin.database().ref('/t_user').indexOf({'idUser' : idUser}).then((snapshot) =>
	{
		'idUser' : idUser,
		'strUser' : strUser,
		'bmSettings' : bmSettings,
		'nRanking' : nRanking,
		'nGames' : nGames
	});
});

// Return a t_user entry based on its ID.
exports.getRandomOpponent = functions.https.onCall((data) =>
{
	const idUser = data.idUser;
	const bmSettings = data.bmSettings;
	const nRanking = data.nRanking;

	// Confirm we have a valid ID.
	if (!Number.isFinite(idUser) ||
		idUser <= 0)
	{
		// Throwing an HttpsError so that the client gets the error details.
		throw new functions.https.HttpsError('invalid-argument', 'Need a valid user ID.');
	}

	for (int nWindow = 0; nWindow <= 100; nWindow += 10)
	{
		

		

	return admin.database().ref('/t_user').indexOf({'idUser' : idUser}).then((snapshot) =>
	{
		'idUser' : idUser,
		'strUser' : strUser,
		'bmSettings' : bmSettings,
		'nRanking' : nRanking,
		'nGames' : nGames
	});
});


exports.dialogflowFirebaseFulfillment = functions.https.onRequest((request, response) =>
{
    console.log('Request headers: ' + JSON.stringify(request.headers));
    console.log('Request body: ' + JSON.stringify(request.body));
 
    // An action is a string used to identify what needs to be done in fulfillment
    let action = request.body.result.action; // https://dialogflow.com/docs/actions-and-parameters
    console.log('Actions = '+ JSON.stringify(action));
 
    let query = request.body.result.resolvedQuery;
 
    // Parameters are any entites that Dialogflow has extracted from the request.
	// https://dialogflow.com/docs/actions-and-parameters
    const parameters = request.body.result.parameters;
 
    // Contexts are objects used to track and store conversation state
    const inputContexts = request.body.result.contexts; // https://dialogflow.com/docs/contexts
 
	if (action === 'firebase.update')
	{
		let userId = 'bert.macklin';
		// Check if the user is in our DB
		admin.firestore().collection('users')
			.where('userId', '==', userId).limit(1).get()
			.then(snapshot =>
			{
				let user = snapshot.docs[0]
				if (!user)
				{
					// Add the user to DB
					admin.firestore().collection('users').add({userId: userId}).then(ref =>
					{
						sendResponse('Added new user');
					});
				}
				else
				{
					// User in DB
					sendResponse('User already exists');
				}
			});
	}
 
	function sendResponse(responseToUser)
	{
		// if the response is a string send it as a response to the user
		if (typeof responseToUser === 'string')
		{
			let responseJson = {};
			responseJson.speech = responseToUser; // spoken response
			responseJson.displayText = responseToUser; // displayed response
			response.json(responseJson); // Send response to Dialogflow
		}
	}


// [START allAdd]
// [START addFunctionTrigger]
// Adds two numbers to each other.
exports.addNumbers = functions.https.onCall((data) => {
// [END addFunctionTrigger]
  // [START readAddData]
  // Numbers passed from the client.
  const firstNumber = data.firstNumber;
  const secondNumber = data.secondNumber;
  // [END readAddData]

  // [START addHttpsError]
  // Checking that attributes are present and are numbers.
  if (!Number.isFinite(firstNumber) || !Number.isFinite(secondNumber)) {
    // Throwing an HttpsError so that the client gets the error details.
    throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
        'two arguments "firstNumber" and "secondNumber" which must both be numbers.');
  }
  // [END addHttpsError]

  // [START returnAddData]
  // returning result.
  return {
    firstNumber: firstNumber,
    secondNumber: secondNumber,
    operator: '+',
    operationResult: firstNumber + secondNumber,
  };
  // [END returnAddData]
});
// [END allAdd]

// [START messageFunctionTrigger]
// Saves a message to the Firebase Realtime Database but sanitizes the text by removing swearwords.
exports.addMessage = functions.https.onCall((data, context) => {
  // [START_EXCLUDE]
  // [START readMessageData]
  // Message text passed from the client.
  const text = data.text;
  // [END readMessageData]
  // [START messageHttpsErrors]
  // Checking attribute.
  if (!(typeof text === 'string') || text.length === 0) {
    // Throwing an HttpsError so that the client gets the error details.
    throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
        'one arguments "text" containing the message text to add.');
  }
  // Checking that the user is authenticated.
  if (!context.auth) {
    // Throwing an HttpsError so that the client gets the error details.
    throw new functions.https.HttpsError('failed-precondition', 'The function must be called ' +
        'while authenticated.');
  }
  // [END messageHttpsErrors]

  // [START authIntegration]
  // Authentication / user information is automatically added to the request.
  const uid = context.auth.uid;
  const name = context.auth.token.name || null;
  const picture = context.auth.token.picture || null;
  const email = context.auth.token.email || null;
  // [END authIntegration]

  // [START returnMessageAsync]
  // Saving the new message to the Realtime Database.
  const sanitizedMessage = sanitizer.sanitizeText(text); // Sanitize the message.
  return admin.database().ref('/messages').push({
    text: sanitizedMessage,
    author: { uid, name, picture, email },
  }).then(() => {
    // Optionally send a push notification with the message.
    if (data.push && context.instanceIdToken) {
      return admin.messaging().send({
        token: context.instanceIdToken,
        data: { text: sanitizedMessage },
      });
    }
  }).then(() => {
    console.log('New Message written');
    return sanitizedMessage;
  }).catch((error) => {
    // Re-throwing the error as an HttpsError so that the client gets the error details.
    throw new functions.https.HttpsError('unknown', error.message, error);
  });
  // [END returnMessageAsync]
  // [END_EXCLUDE]
});
// [END messageFunctionTrigger]
