class MissingServices {
  constructor(serviceList, tIds) {
    this.services = serviceList;
    this.tenantIds = tIds;
  }
}

// MAIN METHOD

let missingServices = [
  {
    name: "Centers for Independent Living",
    cat: "cs01",
    subCat: "scs022",
    airs_cat: "LR;BH",
    airs_subCat: "LR-1550;BH-7000.5100-330;BH-8400.6000-800;LR-3200",
    nameRegex: "/Centers for Independent Living/",
  },
];

const tIds = ["5f572b995d15761b68b1ef0c"];

const finalServices = missingServices.map((service) => {
  try {
    const NPIdentifierObject = new NPdentifiers(service.subCat);
    const value = NPIdentifierObject.getNPIdentifiers();
    // const airsIdentifiers = getAirsAllIdentifiers(service);
    // const identifierList = airsIdentifiers;
    // identifierList.unshift(NPIdentifierObject);
    // return {
    //   tenantIds: tIds,
    //   name: service.name,
    //   identifier: identifierList,
    // };
    print(value);
    return [];
  } catch (err) {
    print("ERROR: " + service.name);
    print(err);
  }
});
// print(finalServices);

function getAirsAllIdentifiers(service) {
  try {
    if (service.airs_subCat !== null) {
      const codes = service.airs_subCat.split(";");
      print(codes);
    }
  } catch (err) {
    print("ERROR: " + service.name);
    print(err);
  }

  return null;
}
