function dataAssociationWithEvent(clientCollection, userCollection, screenerCollection, detailCollection, summaryCollection, event, timelineData, currentTenantId, batchSize) {
    let allTenantsInfo = db.getSiblingDB("dev-qhn-ninepatch-agency").getCollection("tenant").find({}, { name: 1 }).toArray().reduce((accumilator, tenant) => {
        accumilator[tenant._id] = tenant.name;
        return accumilator;
    }, {});
    const allUsers = db.getSiblingDB("dev-qhn-ninepatch-user").getCollection(userCollection).find({}, { "firstName": 1, "lastName": 1, "middleName": 1, "agency": 1, "defaultTenantId": 1, "tenantIds": 1 }).toArray().reduce((accumilator, user) => {
        accumilator[user._id.toString()] = user;
        return accumilator;
    }, {});
    let totalScreeners = db.getCollection(screenerCollection).countDocuments();
    for (let skipValue = 0; skipValue <= totalScreeners; skipValue = skipValue + batchSize) {
        let dataAssociationDetailedDocumentsList = [];
        let dataAssociationSummaryDocumentsList = [];
        let screenersList = db.getCollection(screenerCollection).find({}).skip(skipValue).limit(batchSize).toArray();
        screenersList.forEach(screener => {
            let client = db.getCollection(clientCollection).findOne({ _id: ObjectId(screener?.clientId) });
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
                        let createrUserTenantName = allTenantsInfo[createrUser.defaultTenantId];
                        let modifierUserTenantName = allTenantsInfo[modifierUser.defaultTenantId];
                        let screenerDetailObject = {
                            "client": {
                                "_id": client._id,
                                "firstName": client.firstName,
                                "lastName": client.lastName
                            },
                            "user": {
                                "id": screener?.createdBy,
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
                            "dataAudit": {
                                "created": {
                                    "when": screener.createdAt,
                                    "tenantId": createrUser?.defaultTenantId,
                                    "tenantName": createrUserTenantName,
                                    "entityId": createrUser.agency._id.toString(),
                                    "entityName": createrUser.agency.name,
                                    "userId": createrUser?.createdBy,
                                    "userFullName": createrUser.firstName + " " + createrUser.lastName
                                },
                                "updated": {
                                    "when": screener.lastModifiedAt,
                                    "tenantId": modifierUser?.defaultTenantId,
                                    "tenantName": modifierUserTenantName,
                                    "entityId": modifierUser.agency._id.toString(),
                                    "entityName": modifierUser.agency.name,
                                    "userId": screener.lastModifiedBy,
                                    "userFullName": modifierUser.firstName + " " + modifierUser.lastName
                                }
                            },
                            "_class": "dataAssociationDetail"
                        };
                        dataAssociationDetailedDocumentsList.push(screenerDetailObject);

                        let clientSummaryObject = db.getCollection(summaryCollection).findOne({ "client._id": client._id });
                        //                { associations: { $elemMatch: { "user.id": screener.createdBy } } }
                        if (clientSummaryObject) {
                            let userAssociated = db.getCollection(summaryCollection).findOne({ $and: [{ _id: clientSummaryObject._id }, { associations: { $elemMatch: { "user.id": screener?.createdBy } } }] });
                            if (!userAssociated) {
                                let newAssociations = clientSummaryObject.associations;
                                //                                print(newAssociations.length)
                                let userAssociationDoc = {
                                    "user": screenerDetailObject.user,
                                    "assocDate": screenerDetailObject.assocDate,
                                    "status": screenerDetailObject.status
                                };
                                newAssociations.push(userAssociationDoc);
                                try {
                                    db.getCollection(summaryCollection).updateOne({ _id: clientSummaryObject._id }, { $set: { associations: newAssociations } })
                                    print("User added in associations Successfully..")
                                } catch (e) {
                                    print(`Failure in updation of association of Client${client._id.toString()} with User:${screener?.createdBy}`)
                                    print(e);
                                }
                            } else {
                                print("User is already associated");
                            }
                        } else {
                            //                            print("insert a new summary object for clientTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT");
                            let dataAssociationSummaryDoc = {
                                "client": screenerDetailObject.client,
                                "associations": [],
                                "createdBy": screenerDetailObject.createdBy,
                                "createdAt": screenerDetailObject.createdAt,
                                "lastModifiedBy": screenerDetailObject.lastModifiedBy,
                                "lastModifiedAt": screenerDetailObject.lastModifiedAt,
                                "dataAudit": screenerDetailObject.dataAudit
                            }
                            let associationDoc = {
                                "user": screenerDetailObject.user,
                                "assocDate": screenerDetailObject.assocDate,
                                "status": screenerDetailObject.status
                            }
                            dataAssociationSummaryDoc.associations.push(associationDoc);
                            dataAssociationSummaryDocumentsList.push(dataAssociationSummaryDoc);
                        }

                    } else {
                        print(`${+ !createrUser ? "Creater User: " + createrUser.createdBy : !modifierUser ? "Modifier User: " + screener.lastModifiedBy : ""} not present.`)
                    }
                } else {
                    print(`No Value in createdBy or lastModifiedBy of Screener`);
                }
            } else {
                print(`Client: ${screener.clientId} Not Found for Screener: ${screener._id}`);
            }
        });

        let batchEndValue = skipValue+batchSize <= totalScreeners?skipValue+batchSize:totalScreeners;
        try {
            let detailResponse = db.getCollection(detailCollection).insertMany(dataAssociationDetailedDocumentsList);
            let summaryResponse = db.getCollection(summaryCollection).insertMany(dataAssociationSummaryDocumentsList);
            print(`${Object.values(detailResponse.insertedIds).length} documents inserted into ${detailCollection} collection`);
            print(`${Object.values(summaryResponse.insertedIds).length} documents inserted into ${summaryCollection} collection`);
            print(`Data inserted successfully for Batch Number: ${skipValue} to ${batchEndValue}`);
        } catch (e) {
            print(`Unalbe to insert records for ${skipValue} to ${batchEndValue}`);
            print(e);
        } 

        print(`Processed client from ${skipValue} to ${batchEndValue}`)
    }
}

dataAssociationWithEvent("crn_client", "user", "Tasawar_client_assessment_or_screener", "Tasawar_data_association_detail", "Tasawar_data_association_summary", "SCREENER", true, "5f58aaa8149b3f0006e2e1f7", 15);

