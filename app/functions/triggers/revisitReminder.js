const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
const messaging = admin.messaging();

exports.sendRevisitReminders = functions
	.runWith({
		timeoutSeconds: 540,
		memory: "256MB",
	})
	.pubsub.schedule("0 10 * * 1")
	.timeZone("America/New_York")
	.onRun(async (context) => {
		console.log("Starting revisit reminder notification job...");

		try {
			const usersSnapshot = await db.collection("users").get();
			const notificationPromises = [];

			for (const userDoc of usersSnapshot.docs) {
				const userId = userDoc.id;
				const fcmToken = userDoc.data().fcmToken;

				if (!fcmToken) {
					console.log(`User ${userId} has no FCM token, skipping...`);
					continue;
				}

				const restaurantsToNotify = await checkFiveStarRestaurants(userId);

				if (restaurantsToNotify.length === 0) {
					console.log(`User ${userId} has no restaurants to notify about`);
					continue;
				}

				const notification = createRevisitNotification(restaurantsToNotify);

				const promise = messaging
					.send({
						token: fcmToken,
						notification: {
							title: notification.title,
							body: notification.body,
						},
						data: {
							type: "revisit_reminder",
							title: notification.title,
							body: notification.body,
						},
						android: {
							priority: "high",
							notification: {
								channelId: "taste_tracker_reminders",
								priority: "high",
							},
						},
					})
					.then(() => {
						console.log(
							`Revisit reminder sent successfully to user ${userId}`
						);
					})
					.catch((error) => {
						console.error(
							`Failed to send revisit reminder to user ${userId}:`,
							error
						);
					});

				notificationPromises.push(promise);
			}

			await Promise.all(notificationPromises);
			console.log(
				`Revisit reminder job completed. Sent ${notificationPromises.length} notifications.`
			);
		} catch (error) {
			console.error("Error in revisit reminder job:", error);
			throw error;
		}
	});

exports.testRevisitReminders = functions.https.onRequest(async (req, res) => {
	const userId = req.query.userId;

	if (!userId) {
		res.status(400).send("Missing userId parameter");
		return;
	}

	try {
		const userDoc = await db.collection("users").doc(userId).get();
		const fcmToken = userDoc.data()?.fcmToken;

		if (!fcmToken) {
			res.status(404).send("User has no FCM token");
			return;
		}

		const restaurantsToNotify = await checkFiveStarRestaurants(userId);

		if (restaurantsToNotify.length === 0) {
			res.status(200).json({
				message: "No restaurants to notify about",
				restaurantsFound: [],
			});
			return;
		}

		const notification = createRevisitNotification(restaurantsToNotify);

		await messaging.send({
			token: fcmToken,
			notification: {
				title: notification.title,
				body: notification.body,
			},
			data: {
				type: "revisit_reminder",
				title: notification.title,
				body: notification.body,
			},
		});

		res.status(200).json({
			success: true,
			restaurantsFound: restaurantsToNotify,
			notification,
		});
	} catch (error) {
		console.error("Error in test function:", error);
		res.status(500).send(error.message);
	}
});

async function checkFiveStarRestaurants(userId) {
	const entriesSnapshot = await db
		.collection("users")
		.doc(userId)
		.collection("entries")
		.get();

	if (entriesSnapshot.empty) {
		return [];
	}

	const entries = [];
	entriesSnapshot.forEach((doc) => {
		entries.push(doc.data());
	});

	const fiveStarRestaurants = {};
	entries.forEach((entry) => {
		if (entry.foodQualityRating === 5) {
			const restaurantName = entry.restaurantName;
			if (!fiveStarRestaurants[restaurantName]) {
				fiveStarRestaurants[restaurantName] = [];
			}
			fiveStarRestaurants[restaurantName].push(entry);
		}
	});

	const threeMonthsAgo = new Date();
	threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);

	const restaurantsToNotify = [];

	for (const [restaurantName, restaurantEntries] of Object.entries(
		fiveStarRestaurants
	)) {
		const mostRecentVisit = restaurantEntries.reduce((latest, entry) => {
			const visitDate = entry.visitDate.toDate();
			return !latest || visitDate > latest ? visitDate : latest;
		}, null);

		if (mostRecentVisit && mostRecentVisit < threeMonthsAgo) {
			restaurantsToNotify.push(restaurantName);
		}
	}

	return restaurantsToNotify;
}

function createRevisitNotification(restaurants) {
	const title = "Miss your favorites? 🍽️";

	let body;
	if (restaurants.length === 1) {
		body = `You haven't visited ${restaurants[0]} in over 3 months! Time to go back?`;
	} else if (restaurants.length <= 3) {
		body = `You haven't visited ${restaurants.join(", ")} in over 3 months!`;
	} else {
		body = `You have ${restaurants.length} favorite restaurants you haven't visited in over 3 months!`;
	}

	return { title, body };
}
