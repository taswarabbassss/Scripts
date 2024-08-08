// Removing Duplocate Summary objects based on same user

const collectionName = "data_association_summary";
const clientIds = db
  .getCollection(collectionName)
  .aggregate([
    {
      $group: {
        _id: "$client._id",
        count: {
          $sum: 1,
        },
        docs: {
          $push: "$$ROOT",
        },
      },
    },
    {
      $match: {
        count: {
          $gt: 1,
        },
      },
    },
    {
      $unwind: "$docs",
    },
    {
      $replaceRoot: {
        newRoot: "$docs",
      },
    },
    {
      $group:
        /**
         * specifications: The fields to
         *   include or exclude.
         */
        {
          _id: "",
          ids: {
            $addToSet: "$client._id",
          },
        },
    },
  ])
  .toArray()[0].ids;

const idsList = db
  .getCollection(collectionName)
  .aggregate([
    {
      $group: {
        _id: "$client._id",
        count: {
          $sum: 1,
        },
        docs: {
          $push: "$$ROOT",
        },
      },
    },
    {
      $match: {
        count: {
          $gt: 1,
        },
      },
    },
    {
      $unwind: "$docs",
    },
    {
      $replaceRoot: {
        newRoot: "$docs",
      },
    },
    {
      $group:
        /**
         * specifications: The fields to
         *   include or exclude.
         */
        {
          _id: "",
          ids: {
            $addToSet: "$_id",
          },
        },
    },
  ])
  .toArray()[0].ids;

const docs = db
  .getCollection(collectionName)
  .aggregate([
    {
      $match: {
        _id: { $in: idsList },
      },
    },
    {
      $group: {
        _id: "$client._id",
        client: { $first: "$client" },
        createdBy: { $first: "$createdBy" },
        createdAt: { $first: "$createdAt" },
        lastModifiedBy: { $first: "$lastModifiedBy" },
        lastModifiedAt: { $first: "$lastModifiedAt" },
        dataAudit: { $first: "$dataAudit" },
        associations: { $push: "$associations" },
      },
    },
    {
      $unwind: "$associations",
    },
    {
      $unwind: "$associations",
    },
    {
      $group: {
        _id: {
          client: "$_id",
          userId: "$associations.user.id", // Group by unique user ID
        },
        client: { $first: "$client" },
        createdBy: { $first: "$createdBy" },
        createdAt: { $first: "$createdAt" },
        lastModifiedBy: { $first: "$lastModifiedBy" },
        lastModifiedAt: { $first: "$lastModifiedAt" },
        dataAudit: { $first: "$dataAudit" },
        association: {
          $first: "$associations", // Keep the first association or choose based on some criteria
        },
      },
    },
    {
      $group: {
        _id: "$_id.client",
        client: { $first: "$client" },
        createdBy: { $first: "$createdBy" },
        createdAt: { $first: "$createdAt" },
        lastModifiedBy: { $first: "$lastModifiedBy" },
        lastModifiedAt: { $first: "$lastModifiedAt" },
        dataAudit: { $first: "$dataAudit" },
        associations: { $push: "$association" }, // Rebuild the associations array with unique users
      },
    },
    {
      $project: {
        _id: 1,
        client: 1,
        createdBy: 1,
        createdAt: 1,
        lastModifiedBy: 1,
        lastModifiedAt: 1,
        dataAudit: 1,
        associations: 1,
      },
    },
  ])
  .toArray();

db.getCollection(collectionName).deleteMany({ _id: { $in: idsList } });
db.getCollection(collectionName).insertMany(docs);

print(idsList.length + " Documents Deleted");
print(idsList);

print(docs.length + " Documents Inserted");
print(docs.map((doc) => doc._id));

print("For " + clientIds.length + " Client Ids");
print(clientIds);

///RESULTS

let responseObject1 = { acknowledged: true, deletedCount: 74 };
let responseObject12 = {
  acknowledged: true,
  insertedIds: {
    // '0': ObjectId('661e3db0b5ff2000732ba771'),
    // '1': ObjectId('661e3ca4b5ff2000732ba76c'),
    // '2': ObjectId('66215353431f324e2b6517c9'),
    // '3': ObjectId('65e1b9616ebf4a3d704468d5'),
    // '4': ObjectId('62e0097ae4ab25111ee45533'),
    // '5': ObjectId('662b5ab18607977cfc5b57f5'),
    // '6': ObjectId('667a98e62b4c561956ee8c7e'),
    // '7': ObjectId('662f6eccb4190231e7215671'),
    // '8': ObjectId('65f927b56a164e5c62568ff3'),
    // '9': ObjectId('662bd7d7b4190231e72155d8'),
    // '10': ObjectId('6620c05f431f324e2b6517c6'),
    // '11': ObjectId('663dfebbd7d5c865b20badde'),
    // '12': ObjectId('662bb1b8b4190231e72155c5'),
    // '13': ObjectId('663e058bd7d5c865b20badec'),
    // '14': ObjectId('662b8e6fb4190231e7215566'),
    // '15': ObjectId('6634ca8d830c67448989d96b'),
    // '16': ObjectId('665628edc7e5d24e1e3fefe4'),
    // '17': ObjectId('662a3d5542d7bd011f622b50'),
    // '18': ObjectId('663e003fd7d5c865b20bade2'),
    // '19': ObjectId('662e2284b4190231e72155e1')
  },
};
let DocumentsDeleted = [
  ObjectId("668e6973ca04cdd40b074837"),
  ObjectId("668d10da698cbffe32073e76"),
  ObjectId("668d1111698cbffe32073ece"),
  ObjectId("668d115f698cbffe32073f4a"),
  ObjectId("668ec2692e8e8d670d07347e"),
  ObjectId("668ec2692e8e8d670d07347f"),
  ObjectId("668ec2692e8e8d670d07348d"),
  ObjectId("668d112e698cbffe32073f08"),
  ObjectId("668d112e698cbffe32073f06"),
  ObjectId("668ec2692e8e8d670d07347d"),
  ObjectId("668ec2692e8e8d670d073485"),
  ObjectId("668ec2692e8e8d670d073487"),
  ObjectId("668e6939ca04cdd40b07480c"),
  ObjectId("668e6973ca04cdd40b074838"),
  ObjectId("668d110c698cbffe32073ebc"),
  ObjectId("668d1122698cbffe32073eee"),
  ObjectId("668ec2692e8e8d670d073481"),
  ObjectId("668ec2692e8e8d670d07348b"),
  ObjectId("668d112b698cbffe32073efe"),
  ObjectId("668d1122698cbffe32073eed"),
  ObjectId("668d1109698cbffe32073eb6"),
  ObjectId("668d115f698cbffe32073f49"),
  ObjectId("668e5997ca04cdd40b0746ae"),
  ObjectId("668d1109698cbffe32073eb4"),
  ObjectId("668ec2692e8e8d670d07348f"),
  ObjectId("668ec2692e8e8d670d07348a"),
  ObjectId("668ec2692e8e8d670d07348e"),
  ObjectId("668d110f698cbffe32073ec9"),
  ObjectId("668d10d3698cbffe32073e6a"),
  ObjectId("668d112b698cbffe32073f00"),
  ObjectId("668d10da698cbffe32073e75"),
  ObjectId("668d110c698cbffe32073ebe"),
  ObjectId("668ec2692e8e8d670d073488"),
  ObjectId("668ec2692e8e8d670d073489"),
  ObjectId("668d1128698cbffe32073ef7"),
  ObjectId("668d111e698cbffe32073ee6"),
  ObjectId("668d1119698cbffe32073ede"),
  ObjectId("668d110f698cbffe32073ec8"),
  ObjectId("668d1128698cbffe32073ef8"),
  ObjectId("668d112b698cbffe32073f01"),
  ObjectId("668d1116698cbffe32073ed6"),
  ObjectId("668d1117698cbffe32073eda"),
  ObjectId("668d1109698cbffe32073eb5"),
  ObjectId("668d112b698cbffe32073eff"),
  ObjectId("668d1151698cbffe32073f35"),
  ObjectId("668ec2692e8e8d670d07347c"),
  ObjectId("668ec2692e8e8d670d073490"),
  ObjectId("668ec2692e8e8d670d073491"),
  ObjectId("668e6939ca04cdd40b07480d"),
  ObjectId("668d110f698cbffe32073eca"),
  ObjectId("668d1111698cbffe32073ecd"),
  ObjectId("668ec2692e8e8d670d07348c"),
  ObjectId("668ec2692e8e8d670d073480"),
  ObjectId("668d1117698cbffe32073edb"),
  ObjectId("668d112e698cbffe32073f07"),
  ObjectId("668ec2692e8e8d670d073478"),
  ObjectId("668ec2692e8e8d670d073484"),
  ObjectId("668d1116698cbffe32073ed7"),
  ObjectId("668d1151698cbffe32073f34"),
  ObjectId("668ec2692e8e8d670d07347b"),
  ObjectId("668d110c698cbffe32073ebf"),
  ObjectId("668d1119698cbffe32073edf"),
  ObjectId("668d1128698cbffe32073ef6"),
  ObjectId("668ec2692e8e8d670d073482"),
  ObjectId("668ec2692e8e8d670d073483"),
  ObjectId("668d111e698cbffe32073ee7"),
  ObjectId("668ec2692e8e8d670d073479"),
  ObjectId("668e5997ca04cdd40b0746af"),
  ObjectId("668d110c698cbffe32073ec0"),
  ObjectId("668d110c698cbffe32073ebd"),
  ObjectId("668ec2692e8e8d670d07347a"),
  ObjectId("668e6939ca04cdd40b07480b"),
  ObjectId("668ec2692e8e8d670d073486"),
  ObjectId("668d10d3698cbffe32073e69"),
];

let DocumentsInserted = [
  ObjectId("661e3db0b5ff2000732ba771"),
  ObjectId("661e3ca4b5ff2000732ba76c"),
  ObjectId("66215353431f324e2b6517c9"),
  ObjectId("65e1b9616ebf4a3d704468d5"),
  ObjectId("62e0097ae4ab25111ee45533"),
  ObjectId("662b5ab18607977cfc5b57f5"),
  ObjectId("667a98e62b4c561956ee8c7e"),
  ObjectId("662f6eccb4190231e7215671"),
  ObjectId("65f927b56a164e5c62568ff3"),
  ObjectId("662bd7d7b4190231e72155d8"),
  ObjectId("6620c05f431f324e2b6517c6"),
  ObjectId("663dfebbd7d5c865b20badde"),
  ObjectId("662bb1b8b4190231e72155c5"),
  ObjectId("663e058bd7d5c865b20badec"),
  ObjectId("662b8e6fb4190231e7215566"),
  ObjectId("6634ca8d830c67448989d96b"),
  ObjectId("665628edc7e5d24e1e3fefe4"),
  ObjectId("662a3d5542d7bd011f622b50"),
  ObjectId("663e003fd7d5c865b20bade2"),
  ObjectId("662e2284b4190231e72155e1"),
];

let For20ClientIds = [
  ObjectId("662b8e6fb4190231e7215566"),
  ObjectId("62e0097ae4ab25111ee45533"),
  ObjectId("65e1b9616ebf4a3d704468d5"),
  ObjectId("66215353431f324e2b6517c9"),
  ObjectId("661e3ca4b5ff2000732ba76c"),
  ObjectId("661e3db0b5ff2000732ba771"),
  ObjectId("65f927b56a164e5c62568ff3"),
  ObjectId("662f6eccb4190231e7215671"),
  ObjectId("667a98e62b4c561956ee8c7e"),
  ObjectId("663dfebbd7d5c865b20badde"),
  ObjectId("662b5ab18607977cfc5b57f5"),
  ObjectId("662a3d5542d7bd011f622b50"),
  ObjectId("663e058bd7d5c865b20badec"),
  ObjectId("662bb1b8b4190231e72155c5"),
  ObjectId("6620c05f431f324e2b6517c6"),
  ObjectId("662bd7d7b4190231e72155d8"),
  ObjectId("662e2284b4190231e72155e1"),
  ObjectId("663e003fd7d5c865b20bade2"),
  ObjectId("665628edc7e5d24e1e3fefe4"),
  ObjectId("6634ca8d830c67448989d96b"),
];
