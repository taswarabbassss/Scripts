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

// const localCode = "JR-8200.3000-200";
// const taxonomy_airs_obj = new AirsIdentifiers(localCode);
// taxonomy_airs_obj.getAirsOneIdentifier();
// getAirsOneIdentifier(localCode);
