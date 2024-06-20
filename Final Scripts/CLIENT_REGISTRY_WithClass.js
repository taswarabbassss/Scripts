function setDefaultTenantAndGetNames(
  createrUser,
  modifierUser,
  currentTenantId
) {
  if (!createrUser.defaultTenantId) {
    if (createrUser.tenantIds.length > 0) {
      createrUser.defaultTenantId = createrUser.tenantIds[0];
    } else {
      createrUser.defaultTenantId = currentTenantId;
    }
  }
  if (!modifierUser.defaultTenantId) {
    if (modifierUser.tenantIds.length > 0) {
      modifierUser.defaultTenantId = modifierUser.tenantIds[0];
    } else {
      modifierUser.defaultTenantId = currentTenantId;
    }
  }

  return {
    createrUserTenantName: this.allTenantsInfo[createrUser.defaultTenantId],
    modifierUserTenantName: this.allTenantsInfo[modifierUser.defaultTenantId],
  };
}

function getDetailDocument(
  client,
  createrUser,
  modifierUser,
  event,
  timelineData,
  tenantNames
) {
  try {
    return {
      client: {
        _id: client._id,
        firstName: client.firstName,
        lastName: client.lastName,
      },
      user: {
        id: client.createdBy,
        firstName: createrUser.firstName,
        lastName: createrUser.lastName,
        fullName: createrUser.firstName + " " + createrUser.lastName,
      },
      agency: createrUser.agency,
      assocType: event,
      assocName: event,
      assocDate: client.createdAt,
      sourceId: client._id.toString(),
      timelineData: timelineData,
      status: "ACTIVE",
      createdBy: client.createdBy,
      createdAt: client.createdAt,
      lastModifiedBy: client.lastModifiedBy,
      lastModifiedAt: client.lastModifiedAt,
      dataStatus: "ACTIVE",
      dataAudit: {
        created: {
          when: client.createdAt,
          tenantId: createrUser.defaultTenantId,
          tenantName: tenantNames.createrUserTenantName,
          entityId: createrUser.agency._id.toString(),
          entityName: createrUser.agency.name,
          userId: client.createdBy,
          userFullName: createrUser.firstName + " " + createrUser.lastName,
        },
        updated: {
          when: client.lastModifiedAt,
          tenantId: modifierUser.defaultTenantId,
          tenantName: tenantNames.modifierUserTenantName,
          entityId: modifierUser.agency._id.toString(),
          entityName: modifierUser.agency.name,
          userId: client.lastModifiedBy,
          userFullName: modifierUser.firstName + " " + modifierUser.lastName,
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

function insertSummaryDocuments(
  db,
  summaryDocumentsList,
  summaryCollection,
  totalSummaryDocs,
  skipValue,
  batchEndValue
) {
  try {
    let summaryResponse = db
      .getCollection(summaryCollection)
      .insertMany(summaryDocumentsList);
    totalSummaryDocs =
      totalSummaryDocs + Object.values(summaryResponse.insertedIds).length;
  } catch (e) {
    print(`Unalbe to insert records for ${skipValue} to ${batchEndValue}`);
    print(e);
  }

  return totalSummaryDocs;
}

function insertDetailDocuments(
  db,
  detailDocumentsList,
  detailCollection,
  totalDetailDocs,
  skipValue,
  batchEndValue
) {
  try {
    let detailResponse = db
      .getCollection(detailCollection)
      .insertMany(detailDocumentsList);
    totalDetailDocs =
      totalDetailDocs + Object.values(detailResponse.insertedIds).length;
  } catch (e) {
    print(`Unalbe to insert records for ${skipValue} to ${batchEndValue}`);
    print(e);
  }
}

function getSummaryDocument(dataAssociationDetailDoc) {
  try {
    let dataAssociationSummaryDoc = {
      client: dataAssociationDetailDoc.client,
      associations: [],
      createdBy: dataAssociationDetailDoc.createdBy,
      createdAt: dataAssociationDetailDoc.createdAt,
      lastModifiedBy: dataAssociationDetailDoc.lastModifiedBy,
      lastModifiedAt: dataAssociationDetailDoc.lastModifiedAt,
      dataAudit: dataAssociationDetailDoc.dataAudit,
    };
    let associationDoc = {
      user: dataAssociationDetailDoc.user,
      assocDate: dataAssociationDetailDoc.assocDate,
      status: dataAssociationDetailDoc.status,
    };
    dataAssociationSummaryDoc.associations.push(associationDoc);
    return dataAssociationSummaryDoc;
  } catch (e) {
    print("Error in getSummaryDocument method");
    print(e);
    return {};
  }
}

function finalLogs(detailFaultyDocs, totalDetailDocs, totalSummaryDocs) {
  print(`Total ${detailFaultyDocs} Documents unable to Insert`);
  print(
    `Total ${totalDetailDocs} Documents inserted into {detailCollection} collection`
  );
  print(
    `Total ${totalSummaryDocs} Documents inserted into {summaryCollection} collection`
  );
}

function dataAssociationWithEvent(
  clientCollection,
  userCollection,
  detailCollection,
  summaryCollection,
  event,
  timelineData,
  currentTenantId,
  batchSize
) {
  for (
    let skipValue = 0;
    skipValue <= totalDocumets;
    skipValue = skipValue + batchSize
  ) {
    let clientsList = db
      .getCollection(clientCollection)
      .find({})
      .skip(skipValue)
      .limit(batchSize)
      .toArray();
    clientsList.forEach((client) => {
      let userId = ObjectId(client.createdBy);
      let modifierUserId = ObjectId(client.lastModifiedBy);
      let createrUser = userDb
        .getCollection(userCollection)
        .findOne({ _id: userId });
      let modifierUser =
        userId.toString() === modifierUserId.toString()
          ? createrUser
          : userDb
              .getCollection(userCollection)
              .findOne({ _id: modifierUserId });
      if (createrUser && modifierUser) {
        let tenantNames = setDefaultTenantAndGetNames(
          createrUser,
          modifierUser,
          currentTenantId
        );
        try {
          let dataAssociationDetailDoc = getDetailDocument(
            client,
            createrUser,
            modifierUser,
            event,
            timelineData,
            tenantNames
          );
          detailDocumentsList.push(dataAssociationDetailDoc);
          if (dataAssociationDetailDoc) {
            let dataAssociationSummaryDoc = getSummaryDocument(
              dataAssociationDetailDoc
            );
            summaryDocumentsList.push(dataAssociationSummaryDoc);
          }
        } catch (e) {
          detailFaultyDocs++;
          print(`Error Occured For ${client._id.toString()} Client`);
          print(e);
        }
      } else {
        detailFaultyDocs++;
      }
    });

    let batchEndValue =
      skipValue + batchSize <= totalDocumets
        ? skipValue + batchSize
        : totalDocumets;
    if (detailDocumentsList.length > 0) {
      totalDetailDocs = insertDetailDocuments(
        db,
        detailDocumentsList,
        detailCollection,
        totalDetailDocs,
        skipValue,
        batchEndValue
      );
    }
    if (summaryDocumentsList.length > 0) {
      totalSummaryDocs = insertSummaryDocuments(
        db,
        summaryDocumentsList,
        summaryCollection,
        totalSummaryDocs,
        skipValue,
        batchEndValue
      );
    }
    detailDocumentsList = [];
    summaryDocumentsList = [];
    print(".");
    print(`Processed client from ${skipValue} to ${batchEndValue}`);
  }

  finalLogs(detailFaultyDocs, totalDetailDocs, totalSummaryDocs);
}

dataAssociationWithEvent(
  "Tasawar_crn_client",
  "user",
  "Tasawar_data_association_detail",
  "Tasawar_data_association_summary",
  "CLIENT_REGISTRY",
  true,
  "5f58aaa8149b3f0006e2e1f7",
  50
);

class DataAssociation {
  constructor(
    db,
    clientCollection,
    userCollection,
    detailCollection,
    summaryCollection,
    event,
    currentTenantId,
    batchSize
  ) {
    this.clientCollection = clientCollection;
    this.userCollection = userCollection;
    this.detailCollection = detailCollection;
    this.summaryCollection = summaryCollection;
    this.event = event;
    this.batchSize = batchSize;
    this.currentTenantId = currentTenantId;
    this.allTenantsInfo = db
      .getSiblingDB("qa-shared-ninepatch-agency")
      .getCollection("tenant")
      .find({}, { name: 1 })
      .toArray()
      .reduce((accumilator, tenant) => {
        accumilator[tenant._id] = tenant.name;
        return accumilator;
      }, {});
    this.userDb = db.getSiblingDB("qa-shared-ninepatch-user");
    this.timelineData = true;
    this.detailDocumentsList = [];
    this.summaryDocumentsList = [];
    this.totalDetailDocs = 0;
    this.detailFaultyDocs = 0;
    this.totalSummaryDocs = 0;
    this.totalDocumets = db.getCollection(clientCollection).countDocuments();
  }
}
