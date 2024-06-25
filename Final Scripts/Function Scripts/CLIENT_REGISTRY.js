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
  let userDb = db.getSiblingDB("qa-shared-ninepatch-user");
  let allTenantsInfo = db
    .getSiblingDB("qa-shared-ninepatch-agency")
    .getCollection("tenant")
    .find({}, { name: 1 })
    .toArray()
    .reduce((accumilator, tenant) => {
      accumilator[tenant._id] = tenant.name;
      return accumilator;
    }, {});
  let detailDocumentsList = [];
  let summaryDocumentsList = [];
  let totalDetailDocs = 0;
  let detailFaultyDocs = 0;
  let summaryFaultyDocs = 0;
  let totalSummaryDocs = 0;
  let totalDocumets = db.getCollection(clientCollection).countDocuments();
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
        let createrUserTenantName = allTenantsInfo[createrUser.defaultTenantId];
        let modifierUserTenantName =
          allTenantsInfo[modifierUser.defaultTenantId];
        try {
          let dataAssociationDetailDoc = {
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
            agency: {
              _id: createrUser.agency._id,
              name: createrUser.agency.name,
              nickname: createrUser.agency.nickname,
            },
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
                tenantName: createrUserTenantName,
                entityId: createrUser.agency._id.toString(),
                entityName: createrUser.agency.name,
                userId: client.createdBy,
                userFullName:
                  createrUser.firstName + " " + createrUser.lastName,
              },
              updated: {
                when: client.lastModifiedAt,
                tenantId: modifierUser.defaultTenantId,
                tenantName: modifierUserTenantName,
                entityId: modifierUser.agency._id.toString(),
                entityName: modifierUser.agency.name,
                userId: client.lastModifiedBy,
                userFullName:
                  modifierUser.firstName + " " + modifierUser.lastName,
              },
            },
            _class: "dataAssociationDetail",
          };
          detailDocumentsList.push(dataAssociationDetailDoc);

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

          summaryDocumentsList.push(dataAssociationSummaryDoc);
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
    if (summaryDocumentsList.length > 0) {
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
    }
    detailDocumentsList = [];
    summaryDocumentsList = [];
    print(".");
    print(`Processed client from ${skipValue} to ${batchEndValue}`);
  }

  print(`Total ${detailFaultyDocs} Documents unable to Insert`);
  print(
    `Total ${totalDetailDocs} Documents inserted into {detailCollection} collection`
  );
  print(
    `Total ${totalSummaryDocs} Documents inserted into {summaryCollection} collection`
  );
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
