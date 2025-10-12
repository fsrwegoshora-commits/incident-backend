package com.smartincident.incidentbackend.utils;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@AllArgsConstructor
@NoArgsConstructor
@GraphQLType(name = "PageableParam", description = "Pagination parameters for queries")
public class PageableParam {

    @GraphQLInputField(description = "Field name to sort by")
    private String sortBy;

    @GraphQLInputField(description = "Sort direction (ASC or DESC) as string")
    private String sortDirection;

    @GraphQLInputField(description = "Number of items per page")
    private Integer size;

    @GraphQLInputField(description = "Page number (0-based)")
    private Integer page;

    @GraphQLInputField(description = "Search keyword")
    private String searchParam;

    @GraphQLInputField(description = "Filter by active status")
    private Boolean isActive = true;

    public Pageable getPageable(Boolean sorted) {
        return pageable(sorted, false);
    }

    public Pageable getNativePageable(Boolean sorted) {
        return pageable(sorted, true);
    }

    private Pageable pageable(Boolean sorted, Boolean nativeQuery) {
        if (sortBy == null) sortBy = "createdAt";

        Sort.Direction direction = Sort.Direction.DESC; // Default
        if (sortDirection != null) {
            try {
                direction = Sort.Direction.fromString(sortDirection.toUpperCase());
            } catch (IllegalArgumentException e) {
                // If invalid direction, use default
                direction = Sort.Direction.DESC;
            }
        }

        Sort sort = Sort.by(direction, nativeQuery ? Utils.camelCaseToSnakeCase(sortBy) : sortBy);

        return PageRequest.of(
                page == null || page < 0 ? 0 : page,
                size == null || size < 1 ? ResponsePage.DEFAULT_PAGE_SIZE : size,
                sorted ? sort : Sort.unsorted()
        );
    }

    public void setSort(String sortBy, Sort.Direction sortDirection) {
        this.sortBy = sortBy;
        this.sortDirection = sortDirection != null ? sortDirection.name() : "DESC";
    }

    public Boolean getIsActive() {
        return isActive == null || isActive;
    }

    public String key() {
        return searchParam != null ? searchParam.toLowerCase() : "";
    }
}