class SummaryCollectionisTest {
  constructor(db, userDBName, agencyDBName, collectionName, batchSize) {
    this.batchSize = batchSize;
    this.userDBName = userDBName;
    this.agencyDBName = agencyDBName;
    this.collectionName = collectionName;
    this.clientDb = db;
    this.usersMap = null;
    this.agencyMap = null;
    this.faultyDocs = [];
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
      .countDocuments();
    // this.totalDocs = 2;
    print(`Process started for ${this.totalDocs} documents \n`);
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
        .find({})
        .skip(skip)
        .limit(this.batchSize)
        .toArray();
      let counter = 0;
      documents.forEach((doc) => {
        try {
          const isTestClient = this.testClient(doc?.client?._id + "");
          if (isTestClient === null) {
            this.faultyDocs.push(doc._id);
          }
          const client = { ...doc.client, isTestClient };
          doc.associations.forEach((value, index, array) => {
            let user = value.user;
            let agency = value?.agency;
            const isTestAgency = this.testAgency(agency?._id + "");
            const isTestUser = this.testUser(user.id + "");
            if (isTestAgency === null || isTestUser === null) {
              this.faultyDocs.push(doc._id);
            }
            user = { ...user, isTestUser };
            agency = { ...agency, isTestAgency };
            array[index] = { ...value, user, agency };
          });
          const associations = doc.associations;
          this.clientDb
            .getCollection(this.collectionName)
            .updateOne({ _id: doc._id }, { $set: { client, associations } });
        } catch (err) {
          print("ERROR DOCUMENT ID: " + doc._id);
          print(err);
        }
        print(counter);
        counter++;
      });
      print(`BATCH PROCESSED FROM ${skip} TO ${skip + this.batchSize}`);
      skip += this.batchSize;
    }
    this.faultyDocs = Array.from(new Set(this.faultyDocs));
    print(`FOLLOWING ${this.faultyDocs.length} DOCUMENTS NOT INSETED..`);
    print(this.faultyDocs);
  }
}

const executorMan = new SummaryCollectionisTest(
  db,
  "dev-qhn-ninepatch-user",
  "dev-qhn-ninepatch-agency",
  "test_data_association_summary",
  10
);
executorMan.main();
