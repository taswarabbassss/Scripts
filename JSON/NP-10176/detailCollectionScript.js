class DetailCollectionisTest {
  constructor(
    db,
    userDBName,
    agencyDBName,
    collectionName,
    batchSize,
    findQuery
  ) {
    this.batchSize = batchSize;
    this.userDBName = userDBName;
    this.agencyDBName = agencyDBName;
    this.collectionName = collectionName;
    this.clientDb = db;
    this.usersMap = null;
    this.agencyMap = null;
    this.faultyDocs = [];
    this.findQuery = findQuery;
  }
  postCreationSetup() {
    this.usersMap = this.clientDb
      .getSiblingDB(this.userDBName)
      .getCollection("user")
      .find({}, { isTestUser: 1 })
      .toArray()
      .reduce((acc, doc) => {
        acc[doc._id] = doc;
        return acc;
      }, {});
    this.agencyMap = this.clientDb
      .getSiblingDB(this.agencyDBName)
      .getCollection("agency")
      .find({}, { isTestAgency: 1 })
      .toArray()
      .reduce((acc, doc) => {
        acc[doc._id] = doc;
        return acc;
      }, {});
    print("INITIAL SETUP COMPLETE");
    print("-----------------------------------------\n");
    this.totalDocs = this.clientDb
      .getCollection(this.collectionName)
      .countDocuments(this.findQuery);
    //  this.totalDocs = 10;
    print(`Process started for ${this.totalDocs} documents `);
  }
  testUser(id) {
    try {
      const user = this.usersMap[id];
      if (Object.hasOwn(user, "isTestUser")) {
        return user.isTestUser;
      } else {
        return false;
      }
    } catch (err) {
      print("ERROR_USER_ID: " + id);
      print(err);
      return null;
    }
  }
  testAgency(id) {
    try {
      const agency = this.agencyMap[id];
      if (Object.hasOwn(agency, "isTestAgency")) {
        return agency.isTestAgency;
      } else {
        return false;
      }
    } catch (err) {
      print("ERROR_AGENCY_ID: " + id);
      print(err);
      return null;
    }
  }
  testClient(id) {
    try {
      const client = db
        .getCollection("crn_client")
        .findOne({ _id: ObjectId(id) }, { isTestClient: 1 });
      if (client !== null) {
        if (Object.hasOwn(client, "isTestClient")) {
          return client.isTestClient;
        } else {
          return false;
        }
      } else {
        print("ERROR_CLIENT_ID: " + id + " NOT FOUND.");
        return null;
      }
    } catch (err) {
      print("ERROR_CLIENT_ID: " + id);
      print(err);
      return null;
    }
  }
  main() {
    this.postCreationSetup();
    let skip = 0;
    for (let index = 1; index <= this.totalDocs; index += this.batchSize) {
      const documents = this.clientDb
        .getCollection(this.collectionName)
        .find(this.findQuery)
        .skip(skip)
        .limit(this.batchSize)
        .toArray();
      let counter = 0;
      documents.forEach((doc) => {
        const isTestClient = this.testClient(doc?.client?._id + "");
        const isTestAgency = this.testAgency(doc?.agency?._id + "");
        const isTestUser = this.testUser(doc?.user?.id + "");
        if (
          isTestClient !== null &&
          isTestAgency !== null &&
          isTestUser !== null
        ) {
          const newClient = { ...doc.client, isTestClient: isTestClient };
          const newAgency = { ...doc.agency, isTestAgency: isTestAgency };
          const newUser = { ...doc.user, isTestUser: isTestUser };
          this.clientDb
            .getCollection(this.collectionName)
            .updateOne(
              { _id: doc?._id },
              { $set: { client: newClient, user: newUser, agency: newAgency } }
            );
          print(counter);
        } else {
          print("ERROR DOCUMENT ID: " + doc?._id + "\n");
          this.faultyDocs.push(doc?._id);
        }
        counter++;
      });
      print(`BATCH PROCESSED FROM ${skip} TO ${skip + this.batchSize}`);
      skip += this.batchSize;
    }
    print(`FOLLOWING ${this.faultyDocs.length} DOCUMENTS NOT INSETED..`);
    print(this.faultyDocs);
  }
}

const executorMan = new DetailCollectionisTest(
  db,
  "dev-qhn-ninepatch-user",
  "dev-qhn-ninepatch-agency",
  "data_association_detail",
  50,
  {
    $and: [
      { "client.isTestClient": { $exists: false } },
      { "user.isTestUser": { $exists: false } },
      { "agency.isTestAgency": { $exists: false } },
    ],
  }
);
executorMan.main();
