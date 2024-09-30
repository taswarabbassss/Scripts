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
