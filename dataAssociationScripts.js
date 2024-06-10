//print(db.getCollection("data_association_detail").countDocuments());
//    print(db.getCollection("Tasawar_data_association_detail").find({}).size());


function dataAssociationWithEvent(event, timelineData) {
    var userDb = db.getSiblingDB("dev-qhn-ninepatch-user");
    db.getCollection("Tasawar_crn_client").find({}).forEach(client => {

        var userId = ObjectId(client.createdBy);
        var modifierUserId = ObjectId(client.lastModifiedBy);
        var createrUser = userDb.getCollection("user").findOne({ _id: userId });
        var modifierUser = userDb.getCollection("user").findOne({ _id: modifierUserId });

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
                    "tenantId":createrUser.defaultTenantId,
                    "tenantName": "CRN",
                    "entityId": createrUser.agency._id.toString(),
                    "entityName": createrUser.agency.name,
                    "userId": client.createdBy,
                    "userFullName": createrUser.firstName + " " + createrUser.lastName
                },
                "updated": {
                    "when": client.lastModifiedAt,
                    "tenantId": modifierUser.defaultTenantId,
                    "tenantName": "CRN",
                    "entityId": modifierUser.agency._id.toString(),
                    "entityName": modifierUser.agency.name,
                    "userId": client.lastModifiedBy,
                    "userFullName": modifierUser.firstName + " " + modifierUser.lastName
                }
            },
            "_class" : "dataAssociationDetail"
        };
        print(dataAssociationDetailDoc)
    })
}

dataAssociationWithEvent("CLIENT_REGISTRY", true);





