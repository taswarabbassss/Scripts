class DataAssociation {
  constructor(
    db,
    {
      clientDbName,
      agencyDbName,
      sourceCollection,
      clientCollection,
      userCollection,
      detailCollection,
      summaryCollection,
      event,
      currentTenantId,
      batchSize,
      findQuery,
    }
  ) {
    this.db = db;
    this.clientDb = null;
    this.clientDbName = clientDbName;
    this.agencyDbName = agencyDbName;
    this.sourceCollection = sourceCollection;
    this.clientCollection = clientCollection;
    this.userCollection = userCollection;
    this.detailCollection = detailCollection;
    this.summaryCollection = summaryCollection;
    this.event = event;
    this.batchSize = batchSize;
    this.findQuery = findQuery;
    this.currentTenantId = currentTenantId;
    this.allTenantsInfo = null;
    this.allUsers = null;
    this.timelineData = true;
    this.detailDocumentsList = [];
    this.summaryDocumentsList = [];
    this.totalDetailDocs = 0;
    this.detailFaultyDocs = 0;
    this.insertedAssociations = 0;
    this.detailAlreadyExistingDocuments = 0;
    this.summaryAlreadyAssociatedDocs = 0;
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

  getDetailDocument(
    client,
    user,
    createrUser,
    modifierUser,
    sourceDocument,
    dateCreated,
    dateModified,
    agency
  ) {
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
          _id: agency?._id,
          name: agency?.name,
          nickname: agency?.nickname,
        },
        assocType: this.event,
        assocName: this.event,
        assocDate: dateCreated,
        sourceId: sourceDocument?._id + "",
        timelineData: this.timelineData,
        status: "ACTIVE",
        createdBy: createrUser?._id + "",
        createdAt: dateCreated,
        lastModifiedBy: modifierUser?._id + "",
        lastModifiedAt: dateModified,
        dataStatus: "ACTIVE",
        dataAudit: {
          created: {
            when: dateCreated,
            tenantId: createrUser?.defaultTenantId,
            tenantName: this.allTenantsInfo[createrUser.defaultTenantId],
            entityId: createrUser?.agency?._id + "",
            entityName: createrUser?.agency.name,
            userId: createrUser?._id + "",
            userFullName: createrUser?.firstName + " " + createrUser?.lastName,
          },
          updated: {
            when: dateModified,
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
      const summaryResponse = this.clientDb
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
      const detailResponse = this.clientDb
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
    const clientSummaryObject = this.clientDb
      .getCollection(this.summaryCollection)
      .findOne({ "client._id": client._id });
    if (clientSummaryObject) {
      let userAssociated = this.clientDb
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
          this.clientDb
            .getCollection(this.summaryCollection)
            .updateOne(
              { _id: clientSummaryObject._id },
              { $set: { associations: newAssociations } }
            );
          this.insertedAssociations++;
        } catch (e) {
          print(
            `Failure in updation of association of Client${
              client._id + ""
            } with User:${associatorUserId}`
          );
          print(e);
        }
      } else {
        this.summaryAlreadyAssociatedDocs++;
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
    print(
      `${this.summaryAlreadyAssociatedDocs}: Already  user asssociated summary documents`
    );
    print(
      `${this.totalDetailDocs} Documents inserted into ${this.detailCollection} collection`
    );

    print(`${this.insertedAssociations} Associations inserted`);
    print(
      `${this.totalSummaryDocs} Documents inserted into ${this.summaryCollection} collection`
    );
  }
  getUserWithId(userId) {
    return this.allUsers[userId];
  }
  getClientWithId(clientId) {
    return this.clientDb
      .getCollection(this.clientCollection)
      .findOne(
        { _id: this.getObjectId(clientId) },
        { firstName: 1, lastName: 1 }
      );
  }
  detailDocumentAlreadyExists(userId, clientId, sourceDocumentId) {
    let detailResponse = this.clientDb
      .getCollection(this.detailCollection)
      .findOne({
        "client._id": this.getObjectId(clientId),
        "user.id": userId,
        assocType: this.event,
        sourceId: sourceDocumentId,
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
    associatedUser,
    dateCreated,
    dateModified,
    agency
  ) {
    try {
      const dataAssociationDetailDoc = this.getDetailDocument(
        clientObj,
        associatedUser,
        createrUser,
        modifierUser,
        sourceDocument,
        dateCreated,
        dateModified,
        agency
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
    associatedUser,
    dateCreated,
    dateModified,
    agency
  ) {
    try {
      const dataAssociationDetailDoc = this.getDetailDocument(
        clientObj,
        associatedUser,
        createrUser,
        modifierUser,
        sourceDocument,
        dateCreated,
        dateModified,
        agency
      );
      if (dataAssociationDetailDoc) {
        this.addOrUpdateSummaryDocument(
          clientObj,
          associatedUser._id + "",
          dataAssociationDetailDoc
        );
      }
    } catch (e) {
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
      const sourceDocumentsList = this.db
        .getCollection(this.sourceCollection)
        .find(this.findQuery)
        .skip(skipValue)
        .limit(this.batchSize)
        .toArray();
      sourceDocumentsList.forEach((sourceDocument) => {
        const clientObj = sourceDocument?.client;
        if (clientObj) {
          const createrUser = this.getUserWithId(sourceDocument?.createdBy);
          const modifierUser = this.getUserWithId(
            sourceDocument?.lastModifiedBy
          );
          const associatedUser = createrUser;

          if (createrUser && modifierUser) {
            this.setDefaultTenantId(createrUser, modifierUser);
            const dateCreated = sourceDocument?.createdAt;
            const dateModified = sourceDocument?.lastModifiedAt;
            const agency = createrUser?.agency;
            this.addNewDetailAndSummaryDocument(
              clientObj,
              sourceDocument,
              createrUser,
              modifierUser,
              associatedUser,
              dateCreated,
              dateModified,
              agency
            );
            print(".");
          } else {
            this.detailFaultyDocs++;
          }
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
    this.clientDb = this.db.getSiblingDB(this.clientDbName);
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
      .countDocuments(this.findQuery);
    print(`Process started for ${this.totalDocuments} documents`);
  }
  createMyIndexes() {
    const existingIndexes = this.db
      .getCollection(this.sourceCollection)
      .getIndexes();
    const indexes = Object.keys(this.findQuery).filter(
      (fieldName) => fieldName !== "$expr" || fieldName.includes(".")
    );
    indexes.forEach((indexValue) => {
      const existingIndex = existingIndexes.find(
        (index) => index.key[indexValue]
      );
      if (!existingIndex) {
        const indexName = `data_association_scripting_index_${indexValue}`;
        try {
          this.db
            .getCollection(this.sourceCollection)
            .createIndex({ [indexValue]: 1 }, { name: indexName });
          console.log(`Index ${indexName} created successfully`);
        } catch (e) {
          print(e);
        }
      } else {
        console.log(
          `Index for field ${indexValue} already exists, skipping creation.`
        );
      }
    });
  }
  dropMyIndexes() {
    const existingIndexes = this.db
      .getCollection(this.sourceCollection)
      .getIndexes();
    const indexes = Object.keys(this.findQuery).filter(
      (fieldName) => fieldName !== "$expr" || fieldName.includes(".")
    );
    indexes.forEach((indexValue) => {
      const indexName = `data_association_scripting_index_${indexValue}`;
      const existingIndex = existingIndexes.find(
        (index) => index.name === indexName
      );
      if (existingIndex) {
        this.db.getCollection(this.sourceCollection).dropIndex(indexName);
        console.log(`Index ${indexName} dropped successfully`);
      } else {
        console.log(`Index ${indexName} does not exist, skipping deletion.`);
      }
    });
  }
}

const constructorParameters = {
  clientDbName: "qa-shared-ninepatch-client",
  agencyDbName: "qa-shared-ninepatch-agency",
  sourceCollection: "crn_care_task",
  clientCollection: "crn_client",
  userCollection: "user",
  detailCollection: "data_association_detail",
  summaryCollection: "data_association_summary",
  event: "REFERRAL",
  currentTenantId: "5f572b995d15761b68b1ef0c",
  batchSize: 50,
  findQuery: {
    taskType: "REFERRAL",
    status: { $ne: "DRAFT" },
    client: {
      $exists: true,
    },
    referralType: {
      $in: ["LOG", "OUTOFNETWORK"],
    },
  },
};

const dataAssociationObject = new DataAssociation(db, constructorParameters);
dataAssociationObject.postCreationSetup();
dataAssociationObject.createMyIndexes();
dataAssociationObject.mainDataAssociationMethod();
dataAssociationObject.dropMyIndexes();

/**
    // ---CHANGES---
    CHANGE IN DETAIL Document
    createdAt,createdBy, lastModifiedAt, lastModifiedBy,dataAutdit modifiedAt, dataAudit createdAt
    CHANGE IN THREE Methods
    1) getDetailDocument
    2) addSummaryDocumentWhenDetailDocAlreadyExists (change the dateCreated,dateMofified and agency)
    3) addNewDetailAndSummaryDocument (change the dateCreated,dateMofified and agency)
   */