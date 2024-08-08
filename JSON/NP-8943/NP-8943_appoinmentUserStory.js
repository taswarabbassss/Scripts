let query = [
  {
    $match: {
      $and: [
        {
          clientId: "6684e5bd065e575c95df424e",
          taskType: "APPOINTMENT",
        },
        {
          $or: [
            {
              "client.tenantIds": "5f572b995d15761b68b1ef0c",
            },
            {
              tenantIds: "5f572b995d15761b68b1ef0c",
            },
          ],
        },
      ],
    },
  },
  //   {
  //     $sort: {
  //       createdAt: 1,
  //     },
  //   },
  //   {
  //     $sort: {
  //       createdAt: -1,
  //     },
  //   },
  {
    $skip: 0,
  },
  {
    $limit: 5,
  },
  {
    $project:
      /**
       * specifications: The fields to
       *   include or exclude.
       */
      {
        usersDetails: 1,
      },
  },
  {
    $unwind:
      /**
       * path: Path to the array field.
       * includeArrayIndex: Optional name for index.
       * preserveNullAndEmptyArrays: Optional
       *   toggle to unwind null and empty values.
       */
      {
        path: "$usersDetails",
      },
  },
  {
    $project:
      /**
       * specifications: The fields to
       *   include or exclude.
       */
      {
        agency: "$usersDetails.agency._id",
        _id: 0,
      },
  },
  {
    $group:
      /**
       * _id: The id of the group.
       * fieldN: The first field name.
       */
      {
        _id: null,
        agency: {
          $addToSet: "$agency",
        },
      },
  },
];

let Query2 = [
  {
    $match: {
      $and: [
        {
          clientId: "6684e5bd065e575c95df424e",
          taskType: "APPOINTMENT",
        },
        {
          $or: [
            {
              "client.tenantIds": "5f572b995d15761b68b1ef0c",
            },
            {
              tenantIds: "5f572b995d15761b68b1ef0c",
            },
          ],
        },
      ],
    },
  },
  //   {
  //     $sort: {
  //       createdAt: 1,
  //     },
  //   },
  //   {
  //     $sort: {
  //       createdAt: -1,
  //     },
  //   },
  // {
  //   $skip: 0,
  // },
  // {
  //   $limit: 5,
  // },
  // {
  //   $project:
  //     /**
  //      * specifications: The fields to
  //      *   include or exclude.
  //      */
  //     {
  //       usersDetails: 1,
  //     },
  // },
  {
    $unwind: {
      path: "$usersDetails",
    },
  },
  {
    $project:
      /**
       * specifications: The fields to
       *   include or exclude.
       */
      {
        agencyId: "$usersDetails.agency._id",
        _id: 0,
      },
  },
  {
    $group:
      /**
       * _id: The id of the group.
       * fieldN: The first field name.
       */
      {
        _id: null,
        usersDetails: {
          $addToSet: "$agencyId",
        },
      },
  },
];
