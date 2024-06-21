class DataAssociation {
  constructor(
    db,
    clientCollection,
    userCollection,
    detailCollection,
    summaryCollection,
    event,
    currentTenantId,
    batchSize,
    allTenantsInfo,
    allUsers,
    totalDocumets,
    userDb
  ) {
    this.db = db;
    this.clientCollection = clientCollection;
    this.userCollection = userCollection;
    this.detailCollection = detailCollection;
    this.summaryCollection = summaryCollection;
    this.event = event;
    this.batchSize = batchSize;
    this.currentTenantId = currentTenantId;
    this.allTenantsInfo = allTenantsInfo;
    this.allUsers = allUsers;
    this.userDb = userDb;
    this.timelineData = true;
    this.detailDocumentsList = [];
    this.summaryDocumentsList = [];
    this.totalDetailDocs = 0;
    this.detailFaultyDocs = 0;
    this.totalSummaryDocs = 0;
    this.totalDocumets = totalDocumets;
  }

  setDefaultTenantAndGetNames(createrUser, modifierUser) {
    if (!createrUser.defaultTenantId) {
      if (createrUser.tenantIds.length > 0) {
        createrUser.defaultTenantId = createrUser.tenantIds[0];
      } else {
        createrUser.defaultTenantId = this.currentTenantId;
      }
    }
    if (!modifierUser.defaultTenantId) {
      if (modifierUser.tenantIds.length > 0) {
        modifierUser.defaultTenantId = modifierUser.tenantIds[0];
      } else {
        modifierUser.defaultTenantId = this.currentTenantId;
      }
    }
  }

  getDetailDocument(
    client,
    user,
    createrUser,
    modifierUser,
    eventObject,
    tenantNames
  ) {
    try {
      return {
        client: {
          _id: client?._id,
          firstName: client?.firstName,
          lastName: client?.lastName,
        },
        user: {
          id: user._id.toString(),
          firstName: user?.firstName,
          lastName: user?.lastName,
          fullName: user?.firstName + " " + createrUser?.lastName,
        },
        agency: {
          _id: user?.agency?._id,
          name: user?.agency?.name,
          nickname: user?.agency?.nickname,
        },
        assocType: this.event,
        assocName: this.event,
        assocDate: eventObject?.createdAt,
        sourceId: eventObject?._id.toString(),
        timelineData: this.timelineData,
        status: "ACTIVE",
        createdBy: eventObject?.createdBy,
        createdAt: eventObject?.createdAt,
        lastModifiedBy: eventObject?.lastModifiedBy,
        lastModifiedAt: eventObject?.lastModifiedAt,
        dataStatus: "ACTIVE",
        dataAudit: {
          created: {
            when: eventObject?.createdAt,
            tenantId: createrUser?.defaultTenantId,
            tenantName: this.allTenantsInfo[createrUser.defaultTenantId],
            entityId: createrUser?.agency._id.toString(),
            entityName: createrUser?.agency.name,
            userId: createrUser._id.toString(),
            userFullName: createrUser?.firstName + " " + createrUser?.lastName,
          },
          updated: {
            when: eventObject?.lastModifiedAt,
            tenantId: modifierUser?.defaultTenantId,
            tenantName: this.allTenantsInfo[modifierUser?.defaultTenantId],
            entityId: modifierUser?.agency._id.toString(),
            entityName: modifierUser?.agency.name,
            userId: modifierUser?._id.toString(),
            userFullName:
              modifierUser?.firstName + " " + modifierUser?.lastName,
          },
        },
        _class: "dataAssociationDetail",
      };
    } catch (e) {
      print("Error in getDetailDocument method");
      print(e);
      return {};
    }
  }

  insertSummaryDocuments(skipValue, batchEndValue) {
    try {
      let summaryResponse = db
        .getCollection(this.summaryCollection)
        .insertMany(this.summaryDocumentsList);
      this.totalSummaryDocs =
        this.totalSummaryDocs +
        Object.values(summaryResponse.insertedIds).length;
    } catch (e) {
      print(`Unalbe to insert records for ${skipValue} to ${batchEndValue}`);
      print(e);
    }
    this.summaryDocumentsList = [];
  }

  insertDetailDocuments(skipValue, batchEndValue) {
    try {
      let detailResponse = db
        .getCollection(this.detailCollection)
        .insertMany(this.detailDocumentsList);
      this.totalDetailDocs =
        this.totalDetailDocs + Object.values(detailResponse.insertedIds).length;
    } catch (e) {
      print(`Unalbe to insert records for ${skipValue} to ${batchEndValue}`);
      print(e);
    }
    this.detailDocumentsList = [];
  }

  addOrUpdateSummaryDocument(
    client,
    associatorUserId,
    dataAssociationDetailDoc
  ) {
    let clientSummaryObject = db
      .getCollection(summaryCollection)
      .findOne({ "client._id": client._id });
    if (clientSummaryObject) {
      let userAssociated = db.getCollection(summaryCollection).findOne({
        $and: [
          { _id: clientSummaryObject._id },
          {
            associations: { $elemMatch: { "user.id": associatorUserId } },
          },
        ],
      });
      if (!userAssociated) {
        let newAssociations = clientSummaryObject.associations;
        //                                print(newAssociations.length)
        let userAssociationDoc = {
          user: dataAssociationDetailDoc.user,
          assocDate: dataAssociationDetailDoc.assocDate,
          status: dataAssociationDetailDoc.status,
        };
        newAssociations.push(userAssociationDoc);
        try {
          db.getCollection(summaryCollection).updateOne(
            { _id: clientSummaryObject._id },
            { $set: { associations: newAssociations } }
          );
        } catch (e) {
          print(
            `Failure in updation of association of Client${client._id.toString()} with User:${associatorUserId}`
          );
          print(e);
        }
      }
    } else {
      //                            print("insert a new summary object for clientT");
      try {
        let dataAssociationSummaryDoc = {
          client: dataAssociationDetailDoc?.client,
          associations: [],
          createdBy: dataAssociationDetailDoc?.createdBy,
          createdAt: dataAssociationDetailDoc?.createdAt,
          lastModifiedBy: dataAssociationDetailDoc?.lastModifiedBy,
          lastModifiedAt: dataAssociationDetailDoc?.lastModifiedAt,
          dataAudit: dataAssociationDetailDoc?.dataAudit,
        };
        let associationDoc = {
          user: dataAssociationDetailDoc?.user,
          assocDate: dataAssociationDetailDoc?.assocDate,
          status: dataAssociationDetailDoc?.status,
        };
        dataAssociationSummaryDoc?.associations?.push(associationDoc);
        this.summaryDocumentsList?.push(dataAssociationSummaryDoc);
      } catch (e) {
        print("Error in addOrUpdateSummaryDocument method");
        print(e);
        return {};
      }
    }
  }

  finalLogs() {
    print(`Total ${this.detailFaultyDocs} Documents unable to Insert`);
    print(
      `Total ${this.totalDetailDocs} Documents inserted into {this.detailCollection} collection`
    );
    print(
      `Total ${this.totalSummaryDocs} Documents inserted into {this.summaryCollection} collection`
    );
  }
  getUserWithId(userId) {
    return this.allUsers[userId];
  }
  detailDocumentAlreadyExists(userId, clientId) {
    let detailResponse = this.db
      .getCollection(this.detailCollection)
      .findOne({ "client._id": ObjectId(clientId), "user.id": userId });
    return detailResponse ? true : false;
  }
  mainDataAssociationMethod() {
    for (
      let skipValue = 0;
      skipValue <= totalDocumets;
      skipValue = skipValue + this.batchSize
    ) {
      let clientsList = db
        .getCollection(this.clientCollection)
        .find({})
        .skip(skipValue)
        .limit(this.batchSize)
        .toArray();
      clientsList.forEach((client) => {
        const createrUserId = client.id;
        const modifierUserId = ObjectId(client.lastModifiedBy);
        let createrUser = this.getUserWithId(createrUserId);
        let modifierUser =
          createrUserId.toString() === modifierUserId.toString()
            ? createrUser
            : this.getUserWithId(modifierUserId);
        if (createrUser && modifierUser) {
          this.setDefaultTenantAndGetNames(createrUser, modifierUser);

          client.affiliatedUsers.forEach((affliatedUser) => {
            const user = this.allUsers[affliatedUser.id];
            if (user) {
              if (
                !this.detailDocumentAlreadyExists(
                  user._id.toString(),
                  client.clientId
                )
              ) {
                try {
                  print("OK");
                  //                                let dataAssociationDetailDoc = this.getDetailDocument(
                  //                                    client,
                  //                                    createrUser,
                  //                                    createrUser,
                  //                                    tenantNames
                  //                                );
                  //                                this.detailDocumentsList.push(dataAssociationDetailDoc);
                  //                                if (dataAssociationDetailDoc) {
                  //                                    this.addOrUpdateSummaryDocument(client, client ?.lastModifiedBy, det);
                  //                                }
                } catch (e) {
                  this.detailFaultyDocs++;
                  print(`Error Occured For ${client._id.toString()} Client`);
                  print(e);
                }
              } else {
                print("ALREADY");
                this.detailFaultyDocs++;
              }
            } else {
              this.detailFaultyDocs++;
            }
          });
        } else {
          this.detailFaultyDocs++;
        }
      });

      // let batchEndValue =
      //   skipValue + this.batchSize <= totalDocumets
      //     ? skipValue + this.batchSize
      //     : totalDocumets;
      // if (this.detailDocumentsList.length > 0) {
      //   this.insertDetailDocuments(skipValue, batchEndValue);
      // }
      // if (this.summaryDocumentsList.length > 0) {
      //   this.insertSummaryDocuments(skipValue, batchEndValue);
      // }
      // print(".");
      // print(`Processed client from ${skipValue} to ${batchEndValue}`);
    }

    //        this.finalLogs();
  }
}

const userCollection = "user";
let clientCollection = "Tasawar_program_enrollment";
let allTenantsInfo = db
  .getSiblingDB("qa-shared-ninepatch-agency")
  .getCollection("tenant")
  .find({}, { name: 1 })
  .toArray()
  .reduce((accumilator, tenant) => {
    accumilator[tenant._id] = tenant.name;
    return accumilator;
  }, {});
const allUsers = db
  .getSiblingDB("qa-shared-ninepatch-user")
  .getCollection(userCollection)
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
    accumilator[user._id.toString()] = user;
    return accumilator;
  }, {});
let totalDocumets = db.getCollection(clientCollection).countDocuments();
let userDb = db.getSiblingDB("qa-shared-ninepatch-user");

const dataAssociationObject = new DataAssociation(
  db,
  clientCollection,
  userCollection,
  "Tasawar_data_association_detail",
  "Tasawar_data_association_summary",
  "PROGRAM_ENROLLMENT",
  "5f58aaa8149b3f0006e2e1f7",
  50,
  allTenantsInfo,
  allUsers,
  totalDocumets,
  userDb
);

dataAssociationObject.mainDataAssociationMethod();
