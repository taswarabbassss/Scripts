let dup = [
  {
    $unwind:
      /**
       * path: Path to the array field.
       * includeArrayIndex: Optional name for index.
       * preserveNullAndEmptyArrays: Optional
       *   toggle to unwind null and empty values.
       */
      {
        path: "$affiliatedUsers",
      },
  },
  {
    $group:
      /**
       * _id: The id of the group.
       * fieldN: The first field name.
       */
      {
        _id: {
          docId: "$_id",
          userid: "$affiliatedUsers.id",
        },
        count: {
          $sum: 1,
        },
      },
  },
  {
    $match:
      /**
       * query: The query in MQL.
       */
      {
        count: {
          $gt: 1,
        },
      },
  },
  {
    $group:
      /**
       * _id: The id of the group.
       * fieldN: The first field name.
       */
      {
        _id: "$_id.docId",
        duplicateUsers: {
          $push: {
            userid: "$_id.userid",
            count: "$count",
          },
        },
      },
  },
  {
    $count:
      /**
       * Provide the field name for the count.
       */
      "string",
  },
];
