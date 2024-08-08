let Script = [
  {
    $match: {
      taskType: "REFERRAL",
      status: {
        $ne: "DRAFT",
      },
      client: {
        $exists: true,
      },
      referralType: {
        $nin: ["LOG", "OUTOFNETWORK"],
      },
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
        path: "$history",
      },
  },
  {
    $match:
      /**
       * query: The query in MQL.
       */
      {
        "history.status": {
          $in: ["REQUESTED", "ACCEPTED"],
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
        _id: "$_id",
        history: {
          $push: "$history",
        },
        doc: {
          $first: "$$ROOT",
        },
      },
  },
  {
    $replaceRoot:
      /**
       * replacementDocument: A document or string.
       */
      {
        newRoot: {
          $mergeObjects: [
            "$doc",
            {
              history: "$history",
            },
          ],
        },
      },
  },
  // {
  //   $addFields: {
  //     historyLength: {
  //       $size: "$history"
  //     }
  //   }
  // }
  {
    $match:
      /**
       * query: The query in MQL.
       */
      {
        $expr: {
          $gte: [
            {
              $size: "$history",
            },
            1,
          ],
        },
      },
  },
  // {
  //   $match: {
  //     historyLength: {
  //       $gte: 1
  //     }
  //   }
  // }
  // {
  //   $project: {
  //     historyLength: 0
  //   }
  // }
  // {
  //   $count:
  //     /**
  //      * Provide the field name for the count.
  //      */
  //     "totalHistory"
  // }
];
