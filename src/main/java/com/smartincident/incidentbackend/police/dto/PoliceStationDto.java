package com.smartincident.incidentbackend.police.dto;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@GraphQLType(name = "PoliceStationDto", description = "Input object for police station")
public class PoliceStationDto {

    @GraphQLInputField(name = "uid", description = "Unique identifier for updating existing station")
    private String uid;

    @GraphQLInputField(name = "name", description = "Police station name")
    private String name;

    @GraphQLInputField(name = "contactInfo", description = "Contact information")
    private String contactInfo;

    @GraphQLInputField(name = "administrativeAreaUid", description = "area information")
    private String administrativeAreaUid;

    @GraphQLInputField(name = "location", description = "Location details")
    private LocationDto location;
}