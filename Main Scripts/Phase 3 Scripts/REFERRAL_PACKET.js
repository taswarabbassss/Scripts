const referralCollection = "client_referral_package";
const indexName = `data_association_scripting_index_${updatedAt}`;
db.getCollection(referralCollection).createIndex(
  { updatedAt: 1 },
  { name: indexName }
);
// Add updatedAt field in lastModifiedAt
db.getCollection(referralCollection).updateMany(
  { updatedAt: { $exists: true } },
  [{ $set: { lastModifiedAt: "$updatedAt" } }]
);
// Delete the updatedAt field
db.getCollection(referralCollection).updateMany(
  { updatedAt: { $exists: true } },
  { $unset: { updatedAt: "" } }
);
db.getCollection(referralCollection).dropIndex(indexName);
