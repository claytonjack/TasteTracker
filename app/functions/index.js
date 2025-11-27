const admin = require("firebase-admin");
admin.initializeApp();

const monthlyRecap = require("./triggers/monthlyRecap");
const revisitReminder = require("./triggers/revisitReminder");

exports.sendMonthlyRecap = monthlyRecap.sendMonthlyRecap;
exports.testMonthlyRecap = monthlyRecap.testMonthlyRecap;

exports.sendRevisitReminders = revisitReminder.sendRevisitReminders;
exports.testRevisitReminders = revisitReminder.testRevisitReminders;
