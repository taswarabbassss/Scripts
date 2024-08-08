function dataAssociationWithEvent(
  clientCollection,
  userCollection,
  screenerCollection,
  detailCollection,
  summaryCollection,
  event,
  timelineData,
  currentTenantId,
  batchSize
) {
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
  let totalDocuments = db.getCollection(screenerCollection).countDocuments();
  for (
    let skipValue = 0;
    skipValue <= totalDocuments;
    skipValue = skipValue + batchSize
  ) {
    let dataAssociationDetailedDocumentsList = [];
    let dataAssociationSummaryDocumentsList = [];
    let screenersList = db
      .getCollection(screenerCollection)
      .find({})
      .skip(skipValue)
      .limit(batchSize)
      .toArray();
    screenersList.forEach((screener) => {
      let client = db
        .getCollection(clientCollection)
        .findOne({ _id: ObjectId(screener?.clientId) });
      if (client) {
        if (screener.createdBy && screener.lastModifiedBy) {
          const createrUser = allUsers[screener?.createdBy];
          const modifierUser = allUsers[screener?.lastModifiedBy];
          if (createrUser && modifierUser) {
            if (!createrUser.defaultTenantId) {
              if (createrUser.tenantIds?.length > 0) {
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
            let createrUserTenantName =
              allTenantsInfo[createrUser.defaultTenantId];
            let modifierUserTenantName =
              allTenantsInfo[modifierUser.defaultTenantId];
            let detailObject = {
              client: {
                _id: client._id,
                firstName: client.firstName,
                lastName: client.lastName,
              },
              user: {
                id: screener?.createdBy,
                firstName: createrUser?.firstName,
                lastName: createrUser?.lastName,
                fullName: createrUser?.firstName + " " + createrUser?.lastName,
              },
              agency: {
                _id: createrUser.agency._id,
                name: createrUser.agency.name,
                nickname: createrUser.agency.nickname,
              },
              assocType: event,
              assocName: event,
              assocDate: screener?.createdBy,
              sourceId: screener._id.toString(),
              timelineData: timelineData,
              status: "ACTIVE",
              createdBy: screener?.createdBy,
              createdAt: screener?.createdAt,
              lastModifiedBy: screener.lastModifiedBy,
              lastModifiedAt: screener.lastModifiedAt,
              dataStatus: "ACTIVE",
              dataAudit: {
                created: {
                  when: screener.createdAt,
                  tenantId: createrUser?.defaultTenantId,
                  tenantName: createrUserTenantName,
                  entityId: createrUser.agency._id.toString(),
                  entityName: createrUser.agency.name,
                  userId: screener?.createdBy,
                  userFullName:
                    createrUser.firstName + " " + createrUser.lastName,
                },
                updated: {
                  when: screener.lastModifiedAt,
                  tenantId: modifierUser?.defaultTenantId,
                  tenantName: modifierUserTenantName,
                  entityId: modifierUser.agency._id.toString(),
                  entityName: modifierUser.agency.name,
                  userId: screener.lastModifiedBy,
                  userFullName:
                    modifierUser.firstName + " " + modifierUser.lastName,
                },
              },
              _class: "dataAssociationDetail",
            };
            dataAssociationDetailedDocumentsList.push(detailObject);

            let clientSummaryObject = db
              .getCollection(summaryCollection)
              .findOne({ "client._id": client._id });
            if (clientSummaryObject) {
              let userAssociated = db.getCollection(summaryCollection).findOne({
                $and: [
                  { _id: clientSummaryObject._id },
                  {
                    associations: {
                      $elemMatch: { "user.id": screener?.createdBy },
                    },
                  },
                ],
              });
              if (!userAssociated) {
                let newAssociations = clientSummaryObject.associations;
                //                                print(newAssociations.length)
                let userAssociationDoc = {
                  user: detailObject.user,
                  assocDate: detailObject.assocDate,
                  status: detailObject.status,
                };
                newAssociations.push(userAssociationDoc);
                try {
                  db.getCollection(summaryCollection).updateOne(
                    { _id: clientSummaryObject._id },
                    { $set: { associations: newAssociations } }
                  );
                } catch (e) {
                  print(
                    `Failure in updation of association of Client${client._id.toString()} with User:${
                      screener?.createdBy
                    }`
                  );
                  print(e);
                }
              }
            } else {
              //                            print("insert a new summary object for clientT");
              let dataAssociationSummaryDoc = {
                client: detailObject.client,
                associations: [],
                createdBy: detailObject.createdBy,
                createdAt: detailObject.createdAt,
                lastModifiedBy: detailObject.lastModifiedBy,
                lastModifiedAt: detailObject.lastModifiedAt,
                dataAudit: detailObject.dataAudit,
              };
              let associationDoc = {
                user: detailObject.user,
                assocDate: detailObject.assocDate,
                status: detailObject.status,
              };
              dataAssociationSummaryDoc.associations.push(associationDoc);
              dataAssociationSummaryDocumentsList.push(
                dataAssociationSummaryDoc
              );
            }
          } else {
            print(
              `${
                +!createrUser
                  ? "Creater User: " + createrUser.createdBy
                  : !modifierUser
                  ? "Modifier User: " + screener.lastModifiedBy
                  : ""
              } not present.`
            );
          }
        }
      } else {
        print(
          `Client: ${screener.clientId} Not Found for Screener: ${screener._id}`
        );
      }
    });

    let batchEndValue =
      skipValue + batchSize <= totalDocuments
        ? skipValue + batchSize
        : totalDocuments;
    try {
      const detailResponse = this.db
        .getCollection(detailCollection)
        .insertMany(dataAssociationDetailedDocumentsList);
      const summaryResponse = this.db
        .getCollection(summaryCollection)
        .insertMany(dataAssociationSummaryDocumentsList);
      print(
        `${
          Object.values(detailResponse.insertedIds).length
        } documents inserted into ${detailCollection} collection`
      );
      print(
        `${
          Object.values(summaryResponse.insertedIds).length
        } documents inserted into ${summaryCollection} collection`
      );
      print(
        `Data inserted successfully for Batch Number: ${skipValue} to ${batchEndValue}`
      );
    } catch (e) {
      print(`Unalbe to insert records for ${skipValue} to ${batchEndValue}`);
      print(e);
    }

    print(`Processed client from ${skipValue} to ${batchEndValue}`);
  }
}

dataAssociationWithEvent(
  "crn_client",
  "user",
  "Tasawar_client_assessment_or_screener",
  "data_association_detail",
  "data_association_summary",
  "SCREENER",
  true,
  "5f572b995d15761b68b1ef0c",
  15
);
