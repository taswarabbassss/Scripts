
function dataAssociationWithEvent(clientCollection, userCollection, detailCollection, summaryCollection, event, timelineData, currentTenantId, batchSize) {
    let allTenantsInfo = db.getSiblingDB("qa-shared-ninepatch-agency").getCollection("tenant").find({}, { name: 1 }).toArray().reduce((accumilator, tenant) => {
        accumilator[tenant._id] = tenant.name;
        return accumilator;
    }, {});
    const allUsers = db.getSiblingDB("qa-shared-ninepatch-user").getCollection(userCollection).find({}, { "firstName": 1, "lastName": 1, "middleName": 1, "agency": 1, "defaultTenantId": 1, "tenantIds": 1 }).toArray().reduce((accumilator, user) => {
        accumilator[user._id.toString()] = user;
        return accumilator;
    }, {});
    let detailDocumentsList = [];
    let summaryDocumentsList = [];
    let totalDetailDocs = 0;
    let totalSummaryDocs = 0;
    let totalDocumets = db.getCollection(clientCollection).countDocuments();
    for (let skipValue = 0; skipValue <= totalDocumets; skipValue = skipValue + batchSize) {
        let clientsList = db.getCollection(clientCollection).find({}).skip(skipValue).limit(batchSize).toArray();

        clientsList.forEach(client => {
            if (client.clientDocuments) {
                client.clientDocuments.forEach(document => {
                    if (document.uploadedBy) {
                        let createrUser = allUsers[document.uploadedBy];
                        if (createrUser) {
                            if (!createrUser.defaultTenantId) {
                                if (createrUser.tenantIds ?.length > 0) {
                                    createrUser.defaultTenantId = createrUser.tenantIds[0];
                                } else {
                                    createrUser.defaultTenantId = currentTenantId;
                                }

                            }

                            let createrUserTenantName = allTenantsInfo[createrUser.defaultTenantId];

                            let detailDocument = {
                                "client": {
                                    "_id": client._id,
                                    "firstName": client.firstName,
                                    "lastName": client.lastName
                                },
                                "user": {
                                    "id": document.uploadedBy,
                                    "firstName": createrUser ?.firstName,
                                    "lastName": createrUser ?.lastName,
                                    "fullName": createrUser ?.firstName + " " + createrUser ?.lastName,
                                },
                                "agency": {
                                    "_id": createrUser.agency._id,
                                    "name": createrUser.agency.name,
                                    "nickname": createrUser.agency.nickname
                                },
                                "assocType": event,
                                "assocName": event,
                                "assocDate": document.uploadedAt,
                                "sourceId": client._id.toString(),
                                "timelineData": timelineData,
                                "status": "ACTIVE",
                                "createdBy": document.uploadedBy,
                                "createdAt": document.uploadedAt,
                                "lastModifiedBy": document.uploadedBy,
                                "lastModifiedAt": document.uploadedAt,
                                "dataStatus": "ACTIVE",
                                "dataAudit": {
                                    "created": {
                                        "when": document.uploadedAt,
                                        "tenantId": createrUser ?.defaultTenantId,
                                        "tenantName": createrUserTenantName,
                                        "entityId": createrUser.agency._id.toString(),
                                        "entityName": createrUser.agency.name,
                                        "userId": document.uploadedBy,
                                        "userFullName": createrUser.firstName + " " + createrUser.lastName
                                    },
                                    "updated": {
                                        "when": document.uploadedAt,
                                        "tenantId": createrUser ?.defaultTenantId,
                                        "tenantName": createrUserTenantName,
                                        "entityId": createrUser.agency._id.toString(),
                                        "entityName": createrUser.agency.name,
                                        "userId": document.uploadedBy,
                                        "userFullName": createrUser.firstName + " " + createrUser.lastName
                                    }
                                },
                                "_class": "dataAssociationDetail"
                            };

                            detailDocumentsList.push(detailDocument);

                            let clientSummaryObject = db.getCollection(summaryCollection).findOne({ "client._id": client._id });
                            if (clientSummaryObject) {
                                let userAssociated = db.getCollection(summaryCollection).findOne({ $and: [{ _id: clientSummaryObject._id }, { associations: { $elemMatch: { "user.id": document.uploadedBy } } }] });
                                if (!userAssociated) {
                                    let newAssociations = clientSummaryObject.associations;
                                    let userAssociationDoc = {
                                        "user": detailDocument.user,
                                        "assocDate": detailDocument.assocDate,
                                        "status": detailDocument.status
                                    };
                                    newAssociations.push(userAssociationDoc);
                                    try {
                                        db.getCollection(summaryCollection).updateOne({ _id: clientSummaryObject._id }, { $set: { associations: newAssociations } })
                                    } catch (e) {
                                        print(`Failure in updation of association of Client ${client._id.toString()} with User:${screener ?.createdBy}`)
                                        print(e);
                                    }
                                }
                            } else {
                                //                            print("insert a new summary object for clientT");
                                let summaryDocument = {
                                    "client": detailDocument.client,
                                    "associations": [],
                                    "createdBy": detailDocument.createdBy,
                                    "createdAt": detailDocument.createdAt,
                                    "lastModifiedBy": detailDocument.lastModifiedBy,
                                    "lastModifiedAt": detailDocument.lastModifiedAt,
                                    "dataAudit": detailDocument.dataAudit
                                }
                                let associationDoc = {
                                    "user": detailDocument.user,
                                    "assocDate": detailDocument.assocDate,
                                    "status": detailDocument.status
                                }
                                summaryDocument.associations.push(associationDoc);
                                summaryDocumentsList.push(summaryDocument);
                            }
                        }


                    }
                });

                let batchEndValue = skipValue + batchSize <= totalDocumets ? skipValue + batchSize : totalDocumets;
                if (detailDocumentsList.length > 0) {
                    try {
                        let detailResponse = db.getCollection(detailCollection).insertMany(detailDocumentsList);
                        totalDetailDocs = totalDetailDocs + Object.values(detailResponse.insertedIds).length;
                    } catch (e) {
                        print(`Unalbe to insert records for ${skipValue} to ${batchEndValue}`);
                        print(e);
                    }
                }
                if (summaryDocumentsList.length > 0) {
                    try {
                        let summaryResponse = db.getCollection(summaryCollection).insertMany(summaryDocumentsList);
                        totalSummaryDocs = totalSummaryDocs + Object.values(summaryResponse.insertedIds).length;
                    } catch (e) {
                        print(`Unalbe to insert records for ${skipValue} to ${batchEndValue}`);
                        print(e);
                    }
                }
                detailDocumentsList = [];
                summaryDocumentsList = [];
                print(".");
            }
        });


        let batchEndValue = skipValue + batchSize <= totalDocumets ? skipValue + batchSize : totalDocumets;
        print(`Processed client from ${skipValue} to ${batchEndValue}`);
    }
    print(`Total ${totalDetailDocs} Documents inserted into {detailCollection} collection`);
    print(`Total ${totalSummaryDocs} Documents inserted into {summaryCollection} collection`);
}

dataAssociationWithEvent("Tasawar_crn_client", "user", "Tasawar_data_association_detail", "Tasawar_data_association_summary", "DOC_VAULT", true, "5f58aaa8149b3f0006e2e1f7", 20);

