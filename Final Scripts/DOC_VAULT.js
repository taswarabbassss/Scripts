
function dataAssociationWithEvent(clientCollection, userCollection, detailCollection, summaryCollection, event, timelineData, currentTenantId, batchSize) {
    let userDb = db.getSiblingDB("dev-qhn-ninepatch-user");
    let allTenantsInfo = db.getSiblingDB("dev-qhn-ninepatch-agency").getCollection("tenant").find({}, { name: 1 }).toArray().reduce((accumilator, tenant) => {
        accumilator[tenant._id] = tenant.name;
        return accumilator;
    }, {});
    const allUsers = db.getSiblingDB("dev-qhn-ninepatch-user").getCollection(userCollection).find({}, { "firstName": 1, "lastName": 1, "middleName": 1, "agency": 1, "defaultTenantId": 1, "tenantIds": 1 }).toArray().reduce((accumilator, user) => {
        accumilator[user._id.toString()] = user;
        return accumilator;
    }, {});
    let dataAssociationDetailedDocumentsList = [];
    let dataAssociationSummaryDocumentsList = [];
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
                        }
                        
                        let createrUserTenantName = allTenantsInfo[createrUser.defaultTenantId];
                        
                        let screenerDetailObject = {
                            "client": {
                                "_id": client._id,
                                "firstName": client.firstName,
                                "lastName": client.lastName
                            },
                            "user": {
                                "id": document.uploadedBy,
                                "firstName": createrUser?.firstName,
                                "lastName": createrUser?.lastName,
                                "fullName": createrUser?.firstName + " " + createrUser?.lastName,
                            },
                            "agency": {
                                "_id": createrUser.agency._id,
                                "name": createrUser.agency.name,
                                "nickname": createrUser.agency.nickname
                            },
                            "assocType": event,
                            "assocName": event,
                            "assocDate": client.createdAt,
                            "sourceId": screener._id.toString(),
                            "timelineData": timelineData,
                            "status": "ACTIVE",
                            "createdBy": screener?.createdBy,
                            "createdAt": screener?.createdAt,
                            "lastModifiedBy": screener.lastModifiedBy,
                            "lastModifiedAt": screener.lastModifiedAt,
                            "dataStatus": "ACTIVE",
//                            "dataAudit": {
//                                "created": {
//                                    "when": screener.createdAt,
//                                    "tenantId": createrUser?.defaultTenantId,
//                                    "tenantName": createrUserTenantName,
//                                    "entityId": createrUser.agency._id.toString(),
//                                    "entityName": createrUser.agency.name,
//                                    "userId": createrUser?.createdBy,
//                                    "userFullName": createrUser.firstName + " " + createrUser.lastName
//                                },
//                                "updated": {
//                                    "when": screener.lastModifiedAt,
//                                    "tenantId": modifierUser?.defaultTenantId,
//                                    "tenantName": modifierUserTenantName,
//                                    "entityId": modifierUser.agency._id.toString(),
//                                    "entityName": modifierUser.agency.name,
//                                    "userId": screener.lastModifiedBy,
//                                    "userFullName": modifierUser.firstName + " " + modifierUser.lastName
//                                }
//                            },
                            "_class": "dataAssociationDetail"
                        };
                        
                        dataAssociationDetailedDocumentsList.push(screenerDetailObject);
                    }

                });
                //            let userId = ObjectId(client.createdBy);
                //            let modifierUserId = ObjectId(client.lastModifiedBy);
                //            let createrUser = userDb.getCollection(userCollection).findOne({ _id: userId });
                //            let modifierUser = userId.toString() === modifierUserId.toString() ? createrUser : userDb.getCollection(userCollection).findOne({ _id: modifierUserId });
                //            if (!createrUser.defaultTenantId) {
                //                if (createrUser.tenantIds.length > 0) {
                //                    createrUser.defaultTenantId = createrUser.tenantIds[0];
                //                } else {
                //                    createrUser.defaultTenantId = currentTenantId;
                //                }
                //
                //            }
                //            if (!modifierUser.defaultTenantId) {
                //                if (modifierUser.tenantIds.length > 0) {
                //                    modifierUser.defaultTenantId = modifierUser.tenantIds[0];
                //                } else {
                //                    modifierUser.defaultTenantId = currentTenantId;
                //                }
                //            }
                //            let createrUserTenantName = allTenantsInfo[createrUser.defaultTenantId];
                //            let modifierUserTenantName = allTenantsInfo[modifierUser.defaultTenantId];
                //            if (createrUser && modifierUser) {
                //                let dataAssociationDetailDoc = {
                //                    "client": {
                //                        "_id": client._id,
                //                        "firstName": client.firstName,
                //                        "lastName": client.lastName
                //                    },
                //                    "user": {
                //                        "id": client.createdBy,
                //                        "firstName": createrUser.firstName,
                //                        "lastName": createrUser.lastName,
                //                        "fullName": createrUser.firstName + " " + createrUser.lastName,
                //                    },
                //                    "agency": createrUser.agency,
                //                    "assocType": event,
                //                    "assocName": event,
                //                    "assocDate": client.createdAt,
                //                    "sourceId": client._id.toString(),
                //                    "timelineData": timelineData,
                //                    "status": "ACTIVE",
                //                    "createdBy": client.createdBy,
                //                    "createdAt": client.createdAt,
                //                    "lastModifiedBy": client.lastModifiedBy,
                //                    "lastModifiedAt": client.lastModifiedAt,
                //                    "dataStatus": "ACTIVE",
                //                    "dataAudit": {
                //                        "created": {
                //                            "when": client.createdAt,
                //                            "tenantId": createrUser.defaultTenantId,
                //                            "tenantName": createrUserTenantName,
                //                            "entityId": createrUser.agency._id.toString(),
                //                            "entityName": createrUser.agency.name,
                //                            "userId": client.createdBy,
                //                            "userFullName": createrUser.firstName + " " + createrUser.lastName
                //                        },
                //                        "updated": {
                //                            "when": client.lastModifiedAt,
                //                            "tenantId": modifierUser.defaultTenantId,
                //                            "tenantName": modifierUserTenantName,
                //                            "entityId": modifierUser.agency._id.toString(),
                //                            "entityName": modifierUser.agency.name,
                //                            "userId": client.lastModifiedBy,
                //                            "userFullName": modifierUser.firstName + " " + modifierUser.lastName
                //                        }
                //                    },
                //                    "_class": "dataAssociationDetail"
                //                };
                //
                //                dataAssociationDetailedDocumentsList.push(dataAssociationDetailDoc);
                //
                //                let dataAssociationSummaryDoc = {
                //                    "client": dataAssociationDetailDoc.client,
                //                    "associations": [],
                //                    "createdBy": dataAssociationDetailDoc.createdBy,
                //                    "createdAt": dataAssociationDetailDoc.createdAt,
                //                    "lastModifiedBy": dataAssociationDetailDoc.lastModifiedBy,
                //                    "lastModifiedAt": dataAssociationDetailDoc.lastModifiedAt,
                //                    "dataAudit": dataAssociationDetailDoc.dataAudit
                //                }
                //                let associationDoc = {
                //                    "user": dataAssociationDetailDoc.user,
                //                    "assocDate": dataAssociationDetailDoc.assocDate,
                //                    "status": dataAssociationDetailDoc.status
                //                }
                //                dataAssociationSummaryDoc.associations.push(associationDoc);
                //
                //                dataAssociationSummaryDocumentsList.push(dataAssociationSummaryDoc);
                //
                //
                //            } else {
                //                console.log(`${+ !createrUser ? "Creater User: " + userId.toString() : !modifierUser ? "Modifier User: " + modifierUserId.toString() : ""} not present.`)
                //            }
            }
        });


        let batchEndValue = skipValue + batchSize <= totalDocumets ? skipValue + batchSize : totalDocumets;
        try {
            //            let detailResponse = db.getCollection(detailCollection).insertMany(dataAssociationDetailedDocumentsList);
            //            let summaryResponse = db.getCollection(summaryCollection).insertMany(dataAssociationSummaryDocumentsList);
            //            print(`${Object.values(detailResponse.insertedIds).length} documents inserted into ${detailCollection} collection`);
            //            print(`${Object.values(summaryResponse.insertedIds).length} documents inserted into ${summaryCollection} collection`);
            //            print(`Data inserted successfully for Batch Number: ${skipValue} to ${batchEndValue}`);
        } catch (e) {
            print(`Unalbe to insert records for ${skipValue} to ${batchEndValue}`);
            print(e);
        }
        print(`Processed client from ${skipValue} to ${batchEndValue}`);
    }
}

dataAssociationWithEvent("Tasawar_crn_client", "user", "Tasawar_data_association_detail", "Tasawar_data_association_summary", "DOC_VAULT", true, "5f58aaa8149b3f0006e2e1f7", 20);

