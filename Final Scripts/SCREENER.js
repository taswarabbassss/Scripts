function dataAssociationWithEvent(clientCollection, userCollection, screenerCollection, detailCollection, summaryCollection, event, timelineData, currentTenantId, batchSize) {
    var allTenantsInfo = db.getSiblingDB("dev-qhn-ninepatch-agency").getCollection("tenant").find({}, { name: 1 }).toArray().reduce((accumilator, tenant) => {
        accumilator[tenant._id] = tenant.name;
        return accumilator;
    }, {});
    const allUsers = db.getSiblingDB("dev-qhn-ninepatch-user").getCollection(userCollection).find({}, { "firstName": 1, "lastName": 1, "middleName": 1, "agency": 1, "defaultTenantId": 1, "tenantIds": 1 }).toArray().reduce((accumilator, user) => {
        accumilator[user._id.toString()] = user;
        return accumilator;
    }, {});
    var dataAssociationDetailedDocumentsList = [];
    var dataAssociationSummaryDocumentsList = [];
    const clientsList = db.getCollection(clientCollection).find({}).toArray();
    clientsList.forEach(client => {
        const screenersOfClient = db.getCollection(screenerCollection).find({ clientId: client._id.toString() }).toArray();
        screenersOfClient.forEach(screener => {
            const createrUser = allUsers[screener.createdBy];
            const modifierUser = allUsers[screener.lastModifiedBy];
            if (createrUser && modifierUser) {
                if (!createrUser.defaultTenantId) {
                    if (createrUser.tenantIds ?.length > 0) {
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
                var createrUserTenantName = allTenantsInfo[createrUser.defaultTenantId];
                var modifierUserTenantName = allTenantsInfo[modifierUser.defaultTenantId];
                var screenerDetailObject = {
                    "client": {
                        "_id": client._id,
                        "firstName": client.firstName,
                        "lastName": client.lastName
                    },
                    "user": {
                        "id": screener ?.createdBy,
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
                    "assocDate": client.createdAt,
                    "sourceId": screener._id.toString(),
                    "timelineData": timelineData,
                    "status": "ACTIVE",
                    "createdBy": screener.createdBy,
                    "createdAt": screener.createdAt,
                    "lastModifiedBy": screener.lastModifiedBy,
                    "lastModifiedAt": screener.lastModifiedAt,
                    "dataStatus": "ACTIVE",
                    "dataAudit": {
                        "created": {
                            "when": screener.createdAt,
                            "tenantId": createrUser ?.defaultTenantId,
                            "tenantName": createrUserTenantName,
                            "entityId": createrUser.agency._id.toString(),
                            "entityName": createrUser.agency.name,
                            "userId": createrUser.createdBy,
                            "userFullName": createrUser.firstName + " " + createrUser.lastName
                        },
                        "updated": {
                            "when": screener.lastModifiedAt,
                            "tenantId": modifierUser ?.defaultTenantId,
                            "tenantName": modifierUserTenantName,
                            "entityId": modifierUser.agency._id.toString(),
                            "entityName": modifierUser.agency.name,
                            "userId": screener.lastModifiedBy,
                            "userFullName": modifierUser.firstName + " " + modifierUser.lastName
                        }
                    },
                    "_class": "dataAssociationDetail"
                };

                //                let summaryObject = db.getCollection(summaryCollection).find({ $and: [{ "client._id": client._id }, { associations: { $elemMatch: { "user.id": screener.createdBy } } }] }).toArray();
                print("CLIENT: " + client._id);
                print("USER: " + screener.createdBy);
                var screenerSummaryObject = {};

            } else {
                print(`${+ !createrUser ? "Creater User: " + createrUser.createdBy : !modifierUser ? "Modifier User: " + screener.lastModifiedBy : ""} not present.`)
            }
        });
    });
}

dataAssociationWithEvent("Tasawar_crn_client", "user", "client_assessment_or_screener", "Tasawar_data_association_detail", "Tasawar_data_association_summary", "SCREENER", true, "5f58aaa8149b3f0006e2e1f7", 50);

