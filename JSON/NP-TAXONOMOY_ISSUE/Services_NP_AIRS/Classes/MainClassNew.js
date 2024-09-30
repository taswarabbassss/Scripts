class NPdentifiers {
  constructor(serviceCode) {
    this.code = serviceCode;
    this.identifier = null;
    this.subLevels = [];
  }
  getNPIdentifiers() {
    this.identifier = this.getIdentifierFromTaxonomyFlat(this.code);
    this.getSubLevels();
    let levelNumber = this.subLevels.length;
    this.subLevels = this.subLevels.map((item) => {
      levelNumber--;
      return { ...item, level: levelNumber };
    });
    const finalIdentifier = {
      system: this.identifier.system,
      value: this.identifier.value,
      display: this.identifier.display,
      parentId: this.identifier.parentId,
      level: this.subLevels.length,
      levels: this.subLevels,
      _class: "identifier",
    };

    return finalIdentifier;
  }
  getSubLevels() {
    this.subLevels = [];
    let taxonomyIdenfier = this.identifier;
    do {
      taxonomyIdenfier = this.getIdentifierFromTaxonomyFlat(
        taxonomyIdenfier.parentId
      );
      taxonomyIdenfier !== null ? this.subLevels.push(taxonomyIdenfier) : "";
    } while (taxonomyIdenfier !== null);

    return this.subLevels;
  }
}

class AirsIdentifiers {
  constructor(paramCode) {
    this.code = paramCode;
    this.identifierList = [];
    this.decreaser = -1;
    this.splitters = ["-", ".", "-"];
    this.identifier = null;
    this.nextCode = null;
    this.firstFound = false;
    this.mainObject = null;
  }
  getAirsOneIdentifier(splittedCode) {
    this.code = splittedCode;
    this.identifierList = [];
    const len = this.code.split("-").length;
    this.decreaser =
      len === 3 ? 2 : len === 2 ? (this.code.includes(".") ? 1 : 0) : -1;
    this.identifier = this.getIdentifierFromTaxonomyFlat(this.code);
    this.setMainObject();
    this.getExplicitIdentitifiers();

    this.getTaxonomyIdentifiersList();

    let levelNumber = this.identifierList.length;
    this.mainObject.level = levelNumber;
    this.identifierList = this.identifierList.map((item) => {
      levelNumber--;
      return { ...item, level: levelNumber };
    });
    this.mainObject.levels = this.identifierList;

    return this.mainObject;
  }
  setMainObject() {
    if (this.identifier !== null) {
      this.firstFound = true;
      if (Object.hasOwn(this.identifier, "parentId")) {
        this.mainObject = {
          system: this.identifier.system,
          value: this.identifier.value,
          display: this.identifier.display,
          parentId: this.identifier?.parentId,
          level: 0,
          levels: [],
          _class: "identifier",
        };
        this.nextCode = this.identifier?.parentId;
      } else {
        this.mainObject = {
          system: this.identifier.system,
          value: this.identifier.value,
          display: this.identifier.display,
          level: 0,
          levels: [],
          _class: "identifier",
        };
        this.nextCode = "";
      }
    } else {
      let mainCode = this.code;
      this.code = this.code.slice(
        0,
        this.code.lastIndexOf(this.splitters[this.decreaser])
      );
      this.mainObject = {
        system: "AIRS",
        value: mainCode,
        display: "",
        parentId: this.code,
        level: 0,
        levels: [],
        _class: "identifier",
      };
      this.decreaser--;
    }
  }
  getExplicitIdentitifiers() {
    while (this.decreaser >= 0 && this.identifier === null) {
      let oldCode = this.code;
      this.code = this.code.slice(
        0,
        this.code.lastIndexOf(this.splitters[this.decreaser])
      );
      this.identifier = this.getIdentifierFromTaxonomyFlat(oldCode);
      if (this.identifier !== null) {
        this.mainObject.display = this.identifier.display;
        this.identifierList = this.identifierList.map((item) => {
          return { ...item, display: this.identifier.display };
        });

        this.nextCode = this.identifier.value;
      } else {
        this.identifierList.push({
          value: oldCode,
          parentId: this.code,
          system: "AIRS",
        });
        this.decreaser--;
      }
    }
  }
  getTaxonomyIdentifiersList() {
    try {
      if (this.nextCode) {
        let nextIdentifier = this.getIdentifierFromTaxonomyFlat(this.nextCode);
        this.identifierList.push(nextIdentifier);
        while (Object.hasOwn(nextIdentifier, "parentId")) {
          this.nextCode = nextIdentifier.parentId;
          nextIdentifier = this.getIdentifierFromTaxonomyFlat(this.nextCode);
          this.identifierList.push(nextIdentifier);
        }
      }
    } catch (err) {
      print("ERROR: " + this.nextCode);
      print(err);
    }
  }
  getAirsAllIdentifiers(serviceCodes) {
    const allIdentifiersList = [];
    try {
      const codes = serviceCodes.split(";");
      codes.forEach((code) => {
        const oneIdentObj = this.getAirsOneIdentifier(code);
        if (oneIdentObj !== null) {
          allIdentifiersList.push(oneIdentObj);
        }
      });
    } catch (err) {
      print("ERROR: " + service.name);
      print(err);
    }

    return allIdentifiersList;
  }
}

class FetchTaxonomyFlat {
  constructor() {
    if (!FetchTaxonomyFlat.instance) {
      this.taxomyFlatIdentifiersFromDB = db
        .getCollection("taxonomy")
        .aggregate([
          {
            $unwind: {
              path: "$taxonomyFlat",
            },
          },
          {
            $replaceRoot: {
              newRoot: "$taxonomyFlat",
            },
          },
        ])
        .toArray()
        .reduce((acc, doc) => {
          acc[doc.value] = doc;
          return acc;
        }, {});

      FetchTaxonomyFlat.instance = this;
    }

    return FetchTaxonomyFlat.instance;
  }
  static getInstance() {
    if (!FetchTaxonomyFlat.instance) {
      FetchTaxonomyFlat.instance = new FetchTaxonomyFlat();
    }
    return FetchTaxonomyFlat.instance;
  }
  getFlatIdentifers() {
    return this.taxomyFlatIdentifiersFromDB;
  }

  getIdentifierFromTaxonomyFlat(code) {
    const fetchTaxonomyFlat = FetchTaxonomyFlat.getInstance();
    const identifiersFromDB = fetchTaxonomyFlat.getFlatIdentifers();
    print(identifiersFromDB);
    if (identifiersFromDB && Object.hasOwn(identifiersFromDB, code)) {
      return identifiersFromDB[code];
    }
    return null;
  }
}

function applyMixin(targetClass1, targetClass2, sourceClass) {
  Object.getOwnPropertyNames(sourceClass.prototype).forEach((property) => {
    if (property !== "constructor") {
      targetClass1.prototype[property] = sourceClass.prototype[property];
      targetClass2.prototype[property] = sourceClass.prototype[property];
    }
  });
}

applyMixin(NPdentifiers, AirsIdentifiers, FetchTaxonomyFlat);

class MissingServices {
  constructor(serviceList, tIds, collectionName) {
    this.collectionName = collectionName;
    this.insertServicesNamesList = [];
    this.notInsertedServices = serviceList.filter(
      (item) =>
        item.subCat === null ||
        item.cat === null ||
        item.airs_cat === null ||
        item.airs_subCat === null
    );
    this.missingServices = serviceList.filter(
      (item) =>
        item.subCat !== null &&
        item.cat !== null &&
        item.airs_cat !== null &&
        item.airs_subCat !== null
    );
    this.tenantIds = tIds;
  }

  getNonInsertedServices() {
    return this.notInsertedServices;
  }
  getInsertedServicesNames() {
    return this.insertServicesNamesList;
  }

  insertServices() {
    this.missingServices.map((service) => {
      try {
        const completeService = this.getOneCompleteService(service);
        print(completeService);
        // db.getCollection(this.collectionName).insert(completeService);
        // this.insertServicesNamesList.push(service.name);
        print(".");
      } catch (err) {
        print("ERROR: " + service.name);
        print(err);
      }
    });
  }

  getOneCompleteService(service) {
    const npMan = new NPdentifiers(service.subCat);
    const npIdentifiersList = npMan.getNPIdentifiers();
    const airsMan = new AirsIdentifiers(service.airs_subCat);
    const airsIdentifiers = airsMan.getAirsAllIdentifiers(service.airs_subCat);

    const finalIdentifiers = [npIdentifiersList, ...airsIdentifiers];
    return {
      tenantIds: this.tenantIds,
      name: service.name,
      identifier: finalIdentifiers,
      description: "BLANK",
      type: "Intangible",
      defaultMeasure: "Count",
      defaultUnits: 1,
      defaultValue: 1,
      maxUnits: 0,
      maxValue: 0,
      maxThreshold: 0,
      isLocked: false,
      status: "Active",
      createdBy: "6310a5f615283a509ca3c18e",
      createdAt: new Date(),
      lastModifiedBy: "6310a5f615283a509ca3c18e",
      lastModifiedAt: new Date(),
      _class: "service",
    };
  }
}

let missingServices = [
  {
    name: "Centers for Independent Living",
    cat: "cs01",
    subCat: "scs022",
    airs_cat: "LR;BH",
    airs_subCat: "LR-1550;BH-7000.5100-330;BH-8400.6000-800;LR-3200",
    nameRegex: "/Centers for Independent Living/",
  },
  // {
  //   name: "Referral",
  //   cat: "cs06",
  //   subCat: "scw002",
  //   airs_cat: "TJ",
  //   airs_subCat: null,
  //   nameRegex: "/Referral/",
  // },
];

const tIds = ["5f572b995d15761b68b1ef0c"];
const collectionName = "test_missing_services";
const hulkMan = new MissingServices(missingServices, tIds, collectionName);
hulkMan.insertServices();
const insertedServices = hulkMan.getInsertedServicesNames();
print(insertedServices.length + " Services Inserted Successfully...");
print(insertedServices);
const nonInserted = hulkMan.getNonInsertedServices();
print(nonInserted.length + " Services Not Inserted..");
print(nonInserted);
