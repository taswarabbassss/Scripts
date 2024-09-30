class FetchTaxonomyFlat {
  constructor() {}
  getIdentifierFromTaxonomyFlat(code) {
    const levels = db
      .getCollection("taxonomy")
      .aggregate([
        {
          $unwind: {
            path: "$taxonomyFlat",
          },
        },
        {
          $project: {
            taxonomyFlat: 1,
            _id: 0,
          },
        },
        {
          $match: {
            "taxonomyFlat.value": code,
          },
        },
      ])
      .toArray();

    let level = null;
    if (levels.length > 0) {
      level = levels[0].taxonomyFlat;
    }
    return level;
  }
}
