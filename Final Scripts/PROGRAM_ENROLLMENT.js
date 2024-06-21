class DataAssociation {
  constructor(
    db,
    eventCollection,
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
    this.eventCollection = eventCollection;
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
    this.summaryFaultyDocs = 0;
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

  getDetailDocument(client, user, createrUser, modifierUser, sourceDocument) {
    try {
      return {
        client: {
          _id: client?._id,
          firstName: client?.firstName,
          lastName: client?.lastName,
        },
        user: {
          id: user?._id + "",
          firstName: user?.firstName,
          lastName: user?.lastName,
          fullName: user?.firstName + " " + user?.lastName,
        },
        agency: {
          _id: user?.agency?._id,
          name: user?.agency?.name,
          nickname: user?.agency?.nickname,
        },
        assocType: this.event,
        assocName: this.event,
        assocDate: sourceDocument?.createdAt,
        sourceId: sourceDocument?._id + "",
        timelineData: this.timelineData,
        status: "ACTIVE",
        createdBy: sourceDocument?.createdBy,
        createdAt: sourceDocument?.createdAt,
        lastModifiedBy: sourceDocument?.lastModifiedBy,
        lastModifiedAt: sourceDocument?.lastModifiedAt,
        dataStatus: "ACTIVE",
        dataAudit: {
          created: {
            when: sourceDocument?.createdAt,
            tenantId: createrUser?.defaultTenantId,
            tenantName: this.allTenantsInfo[createrUser.defaultTenantId],
            entityId: createrUser?.agency?._id + "",
            entityName: createrUser?.agency.name,
            userId: createrUser?._id + "",
            userFullName: createrUser?.firstName + " " + createrUser?.lastName,
          },
          updated: {
            when: sourceDocument?.lastModifiedAt,
            tenantId: modifierUser?.defaultTenantId,
            tenantName: this.allTenantsInfo[modifierUser?.defaultTenantId],
            entityId: modifierUser?.agency?._id + "",
            entityName: modifierUser?.agency.name,
            userId: modifierUser?._id + "",
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
    const clientSummaryObject = this.db
      .getCollection(this.summaryCollection)
      .findOne({ "client._id": client._id });
    if (clientSummaryObject) {
      let userAssociated = this.db
        .getCollection(this.summaryCollection)
        .findOne({
          $and: [
            { _id: clientSummaryObject._id },
            {
              associations: { $elemMatch: { "user.id": associatorUserId } },
            },
          ],
        });
      if (!userAssociated) {
            let newAssociations = clientSummaryObject.associations;
            let userAssociationDoc = {
              user: dataAssociationDetailDoc.user,
              assocDate: dataAssociationDetailDoc.assocDate,
              status: dataAssociationDetailDoc.status,
            };
            newAssociations.push(userAssociationDoc);
            try {
              this.db.getCollection(this.summaryCollection).updateOne(
                { _id: clientSummaryObject._id },
                { $set: { associations: newAssociations } }
              );
            } catch (e) {
              print(
                `Failure in updation of association of Client${
                  client._id + ""
                } with User:${associatorUserId}`
              );
              print(e);
            }
      } 
    } else {
      // print("insert a new summary object for clientT");
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
          dataAssociationSummaryDoc.associations.push(associationDoc);
          this.summaryDocumentsList.push(dataAssociationSummaryDoc);
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
  getClientWithId(clientId) {
    return this.db
      .getCollection(this.clientCollection)
      .findOne({ _id: ObjectId(clientId) }, { firstName: 1, lastName: 1 });
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
      let eventDocumentsList = db
        .getCollection(this.eventCollection)
        .find({})
        .skip(skipValue)
        .limit(this.batchSize)
        .toArray();
      eventDocumentsList.forEach((sourceDocument) => {
        const createrUserId = ObjectId(sourceDocument.createdBy);
        const modifierUserId = ObjectId(sourceDocument.lastModifiedBy);
        const createrUser = this.getUserWithId(createrUserId);
        const modifierUser =
          createrUserId.toString() === modifierUserId.toString()
            ? createrUser
            : this.getUserWithId(modifierUserId);
        const clientObj = this.getClientWithId(sourceDocument.clientId);
        if (createrUser && modifierUser && clientObj) {
          this.setDefaultTenantAndGetNames(createrUser, modifierUser);
          sourceDocument.affiliatedUsers.forEach((affliatedUser) => {
            const affiliatedUser = this.allUsers[affliatedUser.id];
            if (affiliatedUser) {
              if (
                !this.detailDocumentAlreadyExists(
                  affiliatedUser._id + "",
                  sourceDocument.clientId
                )
              ) {
                try {
                  const dataAssociationDetailDoc = this.getDetailDocument(
                    clientObj,
                    affiliatedUser,
                    createrUser,
                    modifierUser,
                    sourceDocument
                  );
                  this.detailDocumentsList.push(dataAssociationDetailDoc);
                  if (dataAssociationDetailDoc) {
                    this.addOrUpdateSummaryDocument(
                      clientObj,
                      affiliatedUser._id + "",
                      dataAssociationDetailDoc
                    );
                  }
                } catch (e) {
                  this.detailFaultyDocs++;
                  print(`Error Occured For ${sourceDocument._id + ""} Document`);
                  print(e);
                }
              } else {
                this.detailFaultyDocs++;
              }
              try {
                const detailDoc = this.getDetailDocument(
                  clientObj,
                  affiliatedUser,
                  createrUser,
                  modifierUser,
                  sourceDocument
                );
                if (detailDoc) {
                  this.addOrUpdateSummaryDocument(
                    clientObj,
                    affiliatedUser._id + "",
                    detailDoc
                  );
                }
              } catch (e) {
                this.summaryFaultyDocs++;
                print(`Error Occured For ${sourceDocument._id + ""} Document`);
                print(e);
              }
            } else {
              this.detailFaultyDocs++;
            }
          });


           let batchEndValue =
        skipValue + this.batchSize <= totalDocumets
          ? skipValue + this.batchSize
          : totalDocumets;
      if (this.detailDocumentsList.length > 0) {
        this.insertDetailDocuments(skipValue, batchEndValue);
      }
      if (this.summaryDocumentsList.length > 0) {
        this.insertSummaryDocuments(skipValue, batchEndValue);
      }
      print(".");
        } else {
          this.detailFaultyDocs++;
        }
      });


      let batchEndValue =
        skipValue + this.batchSize <= totalDocumets
          ? skipValue + this.batchSize
          : totalDocumets;
      print(`Processed sourceDocument from ${skipValue} to ${batchEndValue}`);
    }
    this.finalLogs();
  }
}

const userCollection = "user";
let eventCollection = "Tasawar_program_enrollment";
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
let totalDocumets = db.getCollection(eventCollection).countDocuments();
let userDb = db.getSiblingDB("qa-shared-ninepatch-user");

const dataAssociationObject = new DataAssociation(
  db,
  eventCollection,
  "crn_client",
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
