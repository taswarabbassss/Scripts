
function dataAssociationWithEvent(clientCollection, userCollection, detailCollection, summaryCollection, event, timelineData, currentTenantId,batchSize) {
    var userDb = db.getSiblingDB("dev-qhn-ninepatch-user");
    var allTenantsInfo = db.getSiblingDB("dev-qhn-ninepatch-agency").getCollection("tenant").find({}, { name: 1 }).toArray().reduce((accumilator, tenant) => {
        accumilator[tenant._id] = tenant.name;
        return accumilator;
    }, {});
    var dataAssociationDetailedDocumentsList = [];
    var dataAssociationSummaryDocumentsList = [];
    var clientsDocumetsCount = 0;
    var batchNumber = 1;
    var clientsList = db.getCollection(clientCollection).find({}).toArray();
    var secondLastBatchNuber = Math.floor(clientsList.length / batchSize);
    clientsList.forEach(client => {
        var userId = ObjectId(client.createdBy);
        var modifierUserId = ObjectId(client.lastModifiedBy);
        var createrUser = userDb.getCollection(userCollection).findOne({ _id: userId });
        var modifierUser = userId.toString() === modifierUserId.toString() ? createrUser : userDb.getCollection(userCollection).findOne({ _id: modifierUserId });
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
        var createrUserTenantName = allTenantsInfo[createrUser.defaultTenantId];
        var modifierUserTenantName = allTenantsInfo[modifierUser.defaultTenantId];
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
            //        print(dataAssociationDetailDoc);

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
            dataAssociationSummaryDocumentsList.push(dataAssociationSummaryDoc);
            clientsDocumetsCount++;
            if (clientsDocumetsCount >= batchSize || (batchNumber > secondLastBatchNuber && clientsDocumetsCount >= clientsList.length % batchSize)) {
                try {
                    var detailResponse = db.getCollection(detailCollection).insertMany(dataAssociationDetailedDocumentsList);
                    var summaryResponse = db.getCollection(summaryCollection).insertMany(dataAssociationSummaryDocumentsList);
                    print(`${Object.values(detailResponse.insertedIds).length} documents inserted into ${detailCollection} collection`);
                    print(`${Object.values(summaryResponse.insertedIds).length} documents inserted into ${summaryCollection} collection`);
                    print(`Data inserted successfully for Batch Number: ${batchNumber}`);
                } catch (e) {
                    print(`Unalbe to insert records for batch ${batchNumber}`);
                    print(e);
                } finally {
                    batchNumber++;
                    clientsDocumetsCount = 0;
                    dataAssociationDetailedDocumentsList = [];
                    dataAssociationSummaryDocumentsList = [];
                }
            }

        } else {
            console.log(`${+ !createrUser ? "Creater User: " + userId.toString() : !modifierUser ? "Modifier User: " + modifierUserId.toString() : ""} not present.`)
        }
    });
}

dataAssociationWithEvent("Tasawar_crn_client", "user", "Tasawar_data_association_detail", "Tasawar_data_association_summary", "CLIENT_REGISTRY", true, "5f58aaa8149b3f0006e2e1f7",50);

