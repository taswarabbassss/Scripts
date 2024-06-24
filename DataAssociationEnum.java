package com.crn.common.enums;

public enum DataAssociationDetailTypeEnum {
    ED_VISIT(
            "ED_VISIT"),
    DOCTOR_VISIT(
            "DOCTOR_VISIT"),
    UPLOADS(
            "UPLOADS"),
    CONSENT(
            "CONSENT"),
    CLIENT_REGISTRY(
            "CLIENT_REGISTRY"),
    PROGRAM(
            "PROGRAM"),
    INTAKE(
            "INTAKE"),
    DOC_VAULT(
            "DOC_VAULT"),
    CLIENT_DEMOGRAPHICS(
            "CLIENT_DEMOGRAPHICS"),
    SCREENER(
            "SCREENER"),
    REFERRAL_PACKET(
            "REFERRAL_PACKET"),
    SERVICE_TRACKER(
            "SERVICE_TRACKER"),
    ACTION_PLAN(
            "ACTION_PLAN"),
    PROGRAM_ENROLLMENT(
            "PROGRAM_ENROLLMENT"),
    REFERRAL(
            "REFERRAL"),
    TASK(
            "TASK"),
    CASE_NOTE(
            "CASE_NOTE"),
    CARE_TEAM(
            "CARE_TEAM"),
    APPOINTMENT(
            "APPOINTMENT");

    private String value;

    DataAssociationDetailTypeEnum(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}