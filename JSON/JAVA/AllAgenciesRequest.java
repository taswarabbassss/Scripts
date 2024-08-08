package com.crn.agency.model.request;

import lombok.Data;

import java.util.List;

@Data
public class AllAgenciesRequest {
    List<String> allAgencies;
}
