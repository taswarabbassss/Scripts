const referralCollection = "Tasawar_client_referral_package";
db.getCollection(referralCollection).createIndex({ updatedAt: 1 });
db.getCollection(referralCollection).updateMany(
   { updatedAt: { $exists: true } },
   [{ $set: { lastModifiedAt: "$updatedAt" } }]
);
db.getCollection(referralCollection).dropIndex({ updatedAt: 1 });