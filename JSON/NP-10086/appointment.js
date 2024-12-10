[
  {
    $match: {
      $and: [
        { clientId: "664d8033d4c9bd2153f2690e", taskType: "APPOINTMENT" },
        {
          $or: [
            { "client.tenantIds": "5f572b995d15761b68b1ef0c" },
            { tenantIds: "5f572b995d15761b68b1ef0c" },
          ],
        },
      ],
    },
  },
  { $skip: 0 },
  { $limit: 50 },
  { $unwind: "$usersDetails" },
  {
    $match: {
      "usersDetails.agency._id": {
        $in: [
          ObjectId("5eaaabd6149b3f0006514c3f"),
          ObjectId("66472a87d8ada906745af4d8"),
        ],
      },
    },
  },
  {
    $group: {
      _id: "$_id",
      usersDetails: { $push: "$usersDetails" },
      data: { $first: "$$ROOT" },
    },
  },
  { $addFields: { "data.usersDetails": "$usersDetails" } },
  { $replaceRoot: { newRoot: "$data" } },
  { $sort: { "appointmentDateTime.start": -1 } },
];
