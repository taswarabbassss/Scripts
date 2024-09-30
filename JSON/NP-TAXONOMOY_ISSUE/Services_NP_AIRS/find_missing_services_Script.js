const services = [
  {
    name: "Test Services",
    cat: "test01",
    subCat: "test002",
    airs_cat: "RD;RF;RP",
    airs_subCat:
      "test-1900;test-1500;RF-2000;test-2500;sF-3300;RF-8380;RF-8385;RF-8390;RP-1400",
  },
];

// Helper function to escape special regex characters
function escapeRegex(string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

const filteredServices = services
  .map((item) => {
    const nameRegex = new RegExp(escapeRegex(item.name));
    return { ...item, nameRegex };
  })
  .filter((item) => {
    try {
      const data = db
        .getCollection("service")
        .find({
          name: item.nameRegex,
          "identifier.system": "NP",
          "identifier.level": 2,
          "identifier.parentId": item.cat,
          "identifier.value": item.subCat,
        })
        .toArray();
      print(".");
      return data.length < 1;
    } catch (err) {
      print("ERROR IN: " + item.name);
      print(err);
      return true;
    }
  });

print(filteredServices);
print("Total Faulty Combinations: " + filteredServices.length);
