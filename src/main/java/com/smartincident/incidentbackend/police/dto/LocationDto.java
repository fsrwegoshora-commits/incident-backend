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
@GraphQLType(name = "LocationDto", description = "Input object for location")
public class LocationDto {

    @GraphQLInputField(name = "latitude", description = "Latitude coordinate")
    private Double latitude;

    @GraphQLInputField(name = "longitude", description = "Longitude coordinate")
    private Double longitude;

    @GraphQLInputField(name = "address", description = "Physical address")
    private String address;
}