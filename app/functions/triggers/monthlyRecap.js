const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
const messaging = admin.messaging();

exports.sendMonthlyRecap = functions
	.runWith({
		timeoutSeconds: 540,
		memory: "256MB",
	})
	.pubsub.schedule("0 10 1 * *")
	.timeZone("America/New_York")
	.onRun(async (context) => {
		console.log("Starting monthly recap notification job...");

		try {
			const systemAverages = await calculateSystemAverages();
			console.log("System averages calculated:", systemAverages);

			const usersSnapshot = await db.collection("users").get();
			const notificationPromises = [];

			for (const userDoc of usersSnapshot.docs) {
				const userId = userDoc.id;
				const fcmToken = userDoc.data().fcmToken;

				if (!fcmToken) {
					console.log(`User ${userId} has no FCM token, skipping...`);
					continue;
				}

				const userStats = await calculateUserStats(userId);

				if (!userStats) {
					console.log(
						`User ${userId} has no entries last month, skipping...`
					);
					continue;
				}

				const notification = createMonthlyRecapNotification(
					userStats,
					systemAverages
				);

				const promise = messaging
					.send({
						token: fcmToken,
						notification: {
							title: notification.title,
							body: notification.body,
						},
						data: {
							type: "monthly_recap",
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
							`Notification sent successfully to user ${userId}`
						);
					})
					.catch((error) => {
						console.error(
							`Failed to send notification to user ${userId}:`,
							error
						);
					});

				notificationPromises.push(promise);
			}

			await Promise.all(notificationPromises);
			console.log(
				`Monthly recap job completed. Sent ${notificationPromises.length} notifications.`
			);
		} catch (error) {
			console.error("Error in monthly recap job:", error);
			throw error;
		}
	});

exports.testMonthlyRecap = functions.https.onRequest(async (req, res) => {
	const userId = req.query.userId;

	if (!userId) {
		res.status(400).send("Missing userId parameter");
		return;
	}

	try {
		const allEntriesSnapshot = await db.collectionGroup("entries").get();
		const allUserIds = new Set();
		const debugInfo = {
			totalEntries: allEntriesSnapshot.size,
			userIdsFound: [],
			requestedUserId: userId,
		};

		allEntriesSnapshot.forEach((doc) => {
			const entry = doc.data();
			if (entry.userId) {
				allUserIds.add(entry.userId);
			}
		});
		debugInfo.userIdsFound = Array.from(allUserIds);

		console.log("Debug info:", JSON.stringify(debugInfo));

		const systemAverages = await calculateSystemAverages();
		const userStats = await calculateUserStats(userId);

		if (!userStats) {
			res.status(404).json({
				error: "No entries found for user in last month",
				debug: debugInfo,
			});
			return;
		}

		const userDoc = await db.collection("users").doc(userId).get();
		const fcmToken = userDoc.data()?.fcmToken;

		if (!fcmToken) {
			res.status(404).send("User has no FCM token");
			return;
		}

		const notification = createMonthlyRecapNotification(
			userStats,
			systemAverages
		);

		await messaging.send({
			token: fcmToken,
			notification: {
				title: notification.title,
				body: notification.body,
			},
			data: {
				type: "monthly_recap",
				title: notification.title,
				body: notification.body,
			},
		});

		res.status(200).json({
			success: true,
			userStats,
			systemAverages,
			notification,
		});
	} catch (error) {
		console.error("Error in test function:", error);
		res.status(500).send(error.message);
	}
});

async function calculateSystemAverages() {
	const lastMonth = getLastMonthDateRange();

	const entriesSnapshot = await db.collectionGroup("entries").get();

	const lastMonthEntries = [];
	entriesSnapshot.forEach((doc) => {
		const entry = doc.data();
		const visitDate = entry.visitDate.toDate();

		if (visitDate >= lastMonth.start && visitDate <= lastMonth.end) {
			lastMonthEntries.push(entry);
		}
	});

	if (lastMonthEntries.length === 0) {
		return {
			restaurantCount: 0,
			avgRating: 0,
			avgPrice: 0,
			totalUsers: 0,
		};
	}

	const totalRating = lastMonthEntries.reduce(
		(sum, entry) => sum + (entry.foodQualityRating || 0),
		0
	);
	const totalPrice = lastMonthEntries.reduce(
		(sum, entry) => sum + (entry.priceLevel || 0),
		0
	);
	const uniqueUsers = new Set(lastMonthEntries.map((e) => e.userId)).size;

	return {
		restaurantCount: lastMonthEntries.length,
		avgRating: totalRating / lastMonthEntries.length,
		avgPrice: totalPrice / lastMonthEntries.length,
		totalUsers: uniqueUsers,
	};
}

async function calculateUserStats(userId) {
	const lastMonth = getLastMonthDateRange();

	const entriesSnapshot = await db
		.collection("users")
		.doc(userId)
		.collection("entries")
		.get();

	const lastMonthEntries = [];
	entriesSnapshot.forEach((doc) => {
		const entry = doc.data();
		const visitDate = entry.visitDate.toDate();

		if (visitDate >= lastMonth.start && visitDate <= lastMonth.end) {
			lastMonthEntries.push(entry);
		}
	});

	if (lastMonthEntries.length === 0) {
		return null;
	}

	const totalRating = lastMonthEntries.reduce(
		(sum, entry) => sum + (entry.foodQualityRating || 0),
		0
	);
	const totalPrice = lastMonthEntries.reduce(
		(sum, entry) => sum + (entry.priceLevel || 0),
		0
	);

	return {
		restaurantCount: lastMonthEntries.length,
		avgRating: totalRating / lastMonthEntries.length,
		avgPrice: totalPrice / lastMonthEntries.length,
		monthName: lastMonth.monthName,
	};
}

function getLastMonthDateRange() {
	const now = new Date();
	const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
	const startOfLastMonth = new Date(
		lastMonth.getFullYear(),
		lastMonth.getMonth(),
		1
	);
	const endOfLastMonth = new Date(
		lastMonth.getFullYear(),
		lastMonth.getMonth() + 1,
		0,
		23,
		59,
		59
	);

	const monthNames = [
		"January",
		"February",
		"March",
		"April",
		"May",
		"June",
		"July",
		"August",
		"September",
		"October",
		"November",
		"December",
	];

	return {
		start: startOfLastMonth,
		end: endOfLastMonth,
		monthName: monthNames[lastMonth.getMonth()],
	};
}

function createMonthlyRecapNotification(userStats, systemAverages) {
	const title = `${userStats.monthName} Dining Recap 📊`;

	const priceSymbol = (level) => {
		if (level <= 1.5) return "$";
		if (level <= 2.5) return "$$";
		if (level <= 3.5) return "$$$";
		return "$$$$";
	};

	const body =
		`Your Stats:\n` +
		`🍽️ ${userStats.restaurantCount} restaurant${
			userStats.restaurantCount > 1 ? "s" : ""
		} visited\n` +
		`⭐ ${userStats.avgRating.toFixed(1)} stars average\n` +
		`💰 ${priceSymbol(userStats.avgPrice)} average price\n\n\n` +
		`Community Stats:\n` +
		`🍽️ ${systemAverages.restaurantCount} visits by ${systemAverages.totalUsers} users\n` +
		`⭐ ${systemAverages.avgRating.toFixed(1)} stars average\n` +
		`💰 ${priceSymbol(systemAverages.avgPrice)} average price`;

	return { title, body };
}
