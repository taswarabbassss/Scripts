function deleteUsersFromlinkedUserProfileIds(
  collectionName,
  usersList,
  deletedUsersList
) {
  const usersWithObjectId = usersList.map((item) => ObjectId(item));
  const victomUsers = db
    .getCollection(collectionName)
    .find({ _id: { $in: usersWithObjectId } }, { linkedUserProfileIds: 1 });
  const updatedIds = [];
  victomUsers.forEach((doc) => {
    const linkedUsersList = doc.linkedUserProfileIds;
    const filteredUsersList = linkedUsersList.filter(
      (item) => !deletedUsersList.includes(item)
    );
    try {
      db.getCollection(collectionName).updateOne(
        { _id: doc._id },
        { $set: { linkedUserProfileIds: filteredUsersList } }
      );
      updatedIds.push(doc._id + "");
      print(".");
    } catch (err) {
      print("ID: " + doc._id);
      print(err);
    }
  });
  print(updatedIds.length + " Users Updated Successfully...");
  print(updatedIds);
}

const usersList = [
  "61a72c2da7b11b000758e21c",
  "5feac09d9194be0006477ee5",
  "626a71b029ed2d1ce0e6e880",
];
const deletedUsersList = [
  "5fea08d19194be00064e60ef",
  "61434a552ab79c0007f56463",
  "5fea12969194be0006a79fb2",
  "61a73652a7b11b0008e5319c",
];
const collectionName = "user";
deleteUsersFromlinkedUserProfileIds(
  collectionName,
  usersList,
  deletedUsersList
);
