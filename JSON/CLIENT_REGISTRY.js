class DataAssociation {
  constructor(
    db,
    {
      userDbName,
      agencyDbName,
      sourceCollection,
      clientCollection,
      userCollection,
      detailCollection,
      summaryCollection,
      event,
      currentTenantId,
      batchSize,
    }
  ) {
    this.db = db;
    this.userDbName = userDbName;
    this.agencyDbName = agencyDbName;
    this.sourceCollection = sourceCollection;
    this.clientCollection = clientCollection;
    this.userCollection = userCollection;
    this.detailCollection = detailCollection;
    this.summaryCollection = summaryCollection;
    this.event = event;
    this.batchSize = batchSize;
    this.currentTenantId = currentTenantId;
    this.allTenantsInfo = null;
    this.allUsers = null;
    this.timelineData = true;
    this.detailDocumentsList = [];
    this.summaryDocumentsList = [];
    this.totalDetailDocs = 0;
    this.detailFaultyDocs = 0;
    this.detailAlreadyExistingDocuments = 0;
    this.summaryFaultyDocs = 0;
    this.totalSummaryDocs = 0;
    this.totalDocuments = 0;
  }

  setDefaultTenantId(createrUser, modifierUser) {
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
      const summaryResponse = this.db
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
      const detailResponse = this.db
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
          this.db
            .getCollection(this.summaryCollection)
            .updateOne(
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
    print(`Total ${this.totalDocuments}: Documents`);
    print(`${this.detailFaultyDocs}: Detail faulty Documents`);
    print(
      `${this.detailAlreadyExistingDocuments}: Already existing Detail Documents`
    );
    print(`${this.summaryFaultyDocs}: Summary faulty Documents`);
    print(
      `${this.totalDetailDocs} Documents inserted into ${this.detailCollection} collection`
    );
    print(
      `${this.totalSummaryDocs} Documents inserted into ${this.summaryCollection} collection`
    );
  }
  getUserWithId(userId) {
    return this.allUsers[userId];
  }
  getClientWithId(clientId) {
    return this.db
      .getCollection(this.clientCollection)
      .findOne(
        { _id: this.getObjectId(clientId) },
        { firstName: 1, lastName: 1 }
      );
  }
  detailDocumentAlreadyExists(userId, clientId) {
    let detailResponse = this.db.getCollection(this.detailCollection).findOne({
      "client._id": this.getObjectId(clientId),
      "user.id": userId,
      assocType: this.event,
    });
    return detailResponse ? true : false;
  }
  getObjectId(id) {
    let objectId = "";
    try {
      objectId = ObjectId(id);
    } catch (e) {
      print("OBJECT ID ERROR: " + id);
    }
    return objectId;
  }
  addNewDetailAndSummaryDocument(
    clientObj,
    sourceDocument,
    createrUser,
    modifierUser,
    associatedUser
  ) {
    try {
      const dataAssociationDetailDoc = this.getDetailDocument(
        clientObj,
        associatedUser,
        createrUser,
        modifierUser,
        sourceDocument
      );
      this.detailDocumentsList.push(dataAssociationDetailDoc);
      if (dataAssociationDetailDoc) {
        this.addOrUpdateSummaryDocument(
          clientObj,
          associatedUser._id + "",
          dataAssociationDetailDoc
        );
      }
    } catch (e) {
      this.detailFaultyDocs++;
      print(`Error Occured For ${sourceDocument._id + ""} Document`);
      print(e);
    }
  }
  addSummaryDocumentWhenDetailDocAlreadyExists(
    clientObj,
    sourceDocument,
    createrUser,
    modifierUser,
    associatedUser
  ) {
    try {
      const dataAssociationDetailDoc = this.getDetailDocument(
        clientObj,
        associatedUser,
        createrUser,
        modifierUser,
        sourceDocument
      );
      if (dataAssociationDetailDoc) {
        this.addOrUpdateSummaryDocument(
          clientObj,
          associatedUser._id + "",
          dataAssociationDetailDoc
        );
      }
    } catch (e) {
      this.summaryFaultyDocs++;
      print(`Error Occured For ${sourceDocument._id + ""} Document`);
      print(e);
    }
  }
  getBatchEndValue(skipValue) {
    return skipValue + this.batchSize <= this.totalDocuments
      ? skipValue + this.batchSize
      : this.totalDocuments;
  }
  mainDataAssociationMethod() {
    print(this.event);
    for (
      let skipValue = 0;
      skipValue <= this.totalDocuments;
      skipValue = skipValue + this.batchSize
    ) {
      let sourceDocumentsList = db
        .getCollection(this.sourceCollection)
        .find({})
        .skip(skipValue)
        .limit(this.batchSize)
        .toArray();
      sourceDocumentsList.forEach((sourceDocument) => {
        const createrUserId = this.getObjectId(sourceDocument?.createdBy);
        const modifierUserId = this.getObjectId(sourceDocument?.lastModifiedBy);
        const createrUser = this.getUserWithId(createrUserId);
        const modifierUser =
          createrUserId + "" === modifierUserId + ""
            ? createrUser
            : this.getUserWithId(modifierUserId);
        const associatedUser = createrUser;
        const clientObj = sourceDocument;
        if (createrUser && modifierUser && clientObj) {
          this.setDefaultTenantId(createrUser, modifierUser);
          if (
            !this.detailDocumentAlreadyExists(
              associatedUser._id + "",
              clientObj._id + ""
            )
          ) {
            this.addNewDetailAndSummaryDocument(
              clientObj,
              sourceDocument,
              createrUser,
              modifierUser,
              associatedUser
            );
          } else {
            this.detailFaultyDocs++;
            this.addSummaryDocumentWhenDetailDocAlreadyExists(
              clientObj,
              sourceDocument,
              createrUser,
              modifierUser,
              associatedUser
            );
          }
          print(".");
        } else {
          this.detailFaultyDocs++;
        }
      });

      const batchEndValue = this.getBatchEndValue(skipValue);
      if (this.detailDocumentsList.length > 0) {
        print("DETAIL: " + this.detailDocumentsList.length);
        this.insertDetailDocuments(skipValue, batchEndValue);
      }
      if (this.summaryDocumentsList.length > 0) {
        print("SUMMARY: " + this.summaryDocumentsList.length);
        this.insertSummaryDocuments(skipValue, batchEndValue);
      }
      print(`Processed sourceDocument from ${skipValue} to ${batchEndValue}`);
    }
    this.finalLogs();
  }

  postCreationSetup() {
    this.allTenantsInfo = this.db
      .getSiblingDB(this.agencyDbName)
      .getCollection("tenant")
      .find({}, { name: 1 })
      .toArray()
      .reduce((accumilator, tenant) => {
        accumilator[tenant._id] = tenant.name;
        return accumilator;
      }, {});
    this.allUsers = this.db
      .getSiblingDB(this.userDbName)
      .getCollection(this.userCollection)
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
    this.totalDocuments = this.db
      .getCollection(this.sourceCollection)
      .countDocuments();
  }
}

const constructorParameters = {
  userDbName: "qa-shared-ninepatch-user",
  agencyDbName: "qa-shared-ninepatch-agency",
  sourceCollection: "Tasawar_crn_client",
  clientCollection: "crn_client",
  userCollection: "user",
  detailCollection: "data_association_detail",
  summaryCollection: "data_association_summary",
  event: "CLIENT_REGISTRY",
  currentTenantId: "5f572b995d15761b68b1ef0c",
  batchSize: 50,
};

const dataAssociationObject = new DataAssociation(db, constructorParameters);
dataAssociationObject.postCreationSetup();
dataAssociationObject.mainDataAssociationMethod();
