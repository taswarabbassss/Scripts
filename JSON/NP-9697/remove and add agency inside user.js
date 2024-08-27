//1) removal agency from outside user document

db.getCollection("data_association_summary").updateMany(
  { "associations.agency": { $exists: true } },
  { $unset: { "associations.$[].agency": "" } }
);

// 2) adding agency inside user

const clientCollection = "data_association_summary";

const allUsers = db
  .getSiblingDB("dev-rchc-ninepatch-user")
  .getCollection("user")
  .find(
    {},
    {
      firstName: 1,
      lastName: 1,
      middleName: 1,
      agency: 1,
      defaultTenantId: 1,
      tenantIds: 1,
    }
  )
  .toArray()
  .reduce((accumilator, user) => {
    accumilator[user._id + ""] = user;
    return accumilator;
  }, {});

print(allUsers.length);

const summarryDocuments = db.getCollection(clientCollection).find({}).toArray();
let index = 0;
print(summarryDocuments.length);
summarryDocuments.forEach((doc) => {
  let updatedAssociations = [];
  doc.associations.forEach((userDoc) => {
    try {
      const agency = allUsers[userDoc.user.id]?.agency;
      userDoc.user.agency = {
        _id: agency?._id,
        name: agency?.name,
        nickname: agency?.nickname,
      };
    } catch (error) {
      print("ERROR IN ID: " + doc._id);
      prit(error);
    }
    updatedAssociations.push(userDoc);
    print(".");
  });
  doc.associations = updatedAssociations;
  try {
    db.getCollection(clientCollection).updateOne(
      { _id: doc._id },
      { $set: { associations: updatedAssociations } }
    );
  } catch (error) {
    print("ERROR IN ID: " + doc._id);
    prit(error);
  }
  print("# " + index);
  index++;
});
