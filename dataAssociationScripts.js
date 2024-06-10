
function dataAssociationWithEvent(clientCollection, userCollection, detailCollection, summaryCollection, event, timelineData) {
    var userDb = db.getSiblingDB("dev-qhn-ninepatch-user");
    var allTenantsInfo = db.getSiblingDB("dev-qhn-ninepatch-agency").getCollection("tenant").find({}, { name: 1 }).toArray();
    var dataAssociationDetailedDocumentsList = [];
    var dataAssociationSummaryDocumentsList = [];
    db.getCollection(clientCollection).find({}).forEach(client => {
        var userId = ObjectId(client.createdBy);
        var modifierUserId = ObjectId(client.lastModifiedBy);
        var createrUser = userDb.getCollection(userCollection).findOne({ _id: userId });
        var modifierUser = userDb.getCollection(userCollection).findOne({ _id: modifierUserId });
        var createrUserTenantName = allTenantsInfo.filter(tenant => tenant._id.toString() === createrUser.defaultTenantId);
        var modifierUserTenantName = allTenantsInfo.filter(tenant => tenant._id.toString() === modifierUser.defaultTenantId);
        if (createrUser && modifierUser) {
            var dataAssociationDetailDoc = {
                "client": {
                    "_id": client._id,
                    "firstName": client.firstName,
                    "lastName": client.lastName
                },
                "user": {
                    "id": client.createdBy,
                    "firstName": createrUser.firstName,
                    "lastName": createrUser.lastName,
                    "fullName": createrUser.firstName + " " + createrUser.lastName,
                    "isAddByCaseLoad": false
                },
                "agency": createrUser.agency,
                "assocType": event,
                "assocName": event,
                "assocDate": client.createdAt,
                "sourceId": client._id.toString(),
                "timelineData": timelineData,
                "status": "ACTIVE",
                "createdBy": client.createdBy,
                "createdAt": client.createdAt,
                "lastModifiedBy": client.lastModifiedBy,
                "lastModifiedAt": client.lastModifiedAt,
                "dataStatus": "ACTIVE",
                "dataAudit": {
                    "created": {
                        "when": client.createdAt,
                        "tenantId": createrUser.defaultTenantId,
                        "tenantName": createrUserTenantName,
                        "entityId": createrUser.agency._id.toString(),
                        "entityName": createrUser.agency.name,
                        "userId": client.createdBy,
                        "userFullName": createrUser.firstName + " " + createrUser.lastName
                    },
                    "updated": {
                        "when": client.lastModifiedAt,
                        "tenantId": modifierUser.defaultTenantId,
                        "tenantName": modifierUserTenantName,
                        "entityId": modifierUser.agency._id.toString(),
                        "entityName": modifierUser.agency.name,
                        "userId": client.lastModifiedBy,
                        "userFullName": modifierUser.firstName + " " + modifierUser.lastName
                    }
                },
                "_class": "dataAssociationDetail"
            };
            var dataAssociationSummaryDoc = {
                "client": dataAssociationDetailDoc.client,
                "associations": [],
                "createdBy": dataAssociationDetailDoc.createdBy,
                "createdAt": dataAssociationDetailDoc.createdAt,
                "lastModifiedBy": dataAssociationDetailDoc.lastModifiedBy,
                "lastModifiedAt": dataAssociationDetailDoc.lastModifiedAt,
                "dataAudit": dataAssociationDetailDoc.dataAudit
            }
            var associationDoc = {
                "user": dataAssociationDetailDoc.user,
                "assocDate": dataAssociationDetailDoc.assocDate,
                "status": dataAssociationDetailDoc.status
            }
            dataAssociationSummaryDoc.associations.push(associationDoc);

            dataAssociationDetailedDocumentsList.push(dataAssociationDetailDoc);
            dataAssociationSummaryDocumentsList.push(dataAssociationDetailDoc);
        } else {
            console.log(`${+ !createrUser ? "Creater User: " + userId.toString() : !modifierUser ? "Modifier User: " + modifierUserId.toString() : ""} not present.`)
        }
    });
    try {
        var detailResponse = db.getCollection(detailCollection).insertMany(dataAssociationDetailedDocumentsList);
        var summaryResponse = db.getCollection(summaryCollection).insertMany(dataAssociationSummaryDocumentsList);
        print(`${Object.values(detailResponse.insertedIds).length} documents inserted into ${detailCollection} collection`);
        print(`${Object.values(summaryResponse.insertedIds).length} documents inserted into ${summaryCollection} collection`);
    } catch (e) {
        print(e);
    }
}

dataAssociationWithEvent("Tasawar_crn_client", "user", "Tasawar_data_association_detail", "Tasawar_data_association_summary", "CLIENT_REGISTRY", true);

