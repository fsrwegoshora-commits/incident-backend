package com.smartincident.incidentbackend.entity;

import com.smartincident.incidentbackend.utils.Utils;
import io.leangen.graphql.annotations.GraphQLQuery;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrandBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uid", nullable = false, unique = true)
    private String uid = Utils.generateUniqueID();

    @GraphQLQuery(name = "id")
    public Long getId() {
        return id;
    }

    @GraphQLQuery(name = "uid")
    public String getUid() {
        return uid;
    }
}
