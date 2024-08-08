addFields()
  .addField("data." + USER_DETAILS)
  .withValue("$" + USER_DETAILS)
  .build(),
  replaceRoot().withValueOf("$data");
