const summaryIndexes = ["_id", "client._id", "user.id", "user.agency._id"];

summaryIndexes.forEach((ind) => {
  db.getCollection("data_association_summary").createIndex({ [ind]: 1 });
});

const detailIndexes = [
  "_id",
  "client._id",
  "user.id",
  "agency._id",
  "assocType",
];

detailIndexes.forEach((ind) => {
  db.getCollection("data_association_detail").createIndex({ [ind]: 1 });
});
