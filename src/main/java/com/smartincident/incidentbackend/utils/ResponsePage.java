package com.smartincident.incidentbackend.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Data
@AllArgsConstructor
@ToString
@GraphQLType
public class ResponsePage<T> extends Response<List<T>> {

    @GraphQLIgnore
    @JsonIgnore
    public static final Integer DEFAULT_PAGE_SIZE = 10;
    private Integer elements;
    private Integer size = DEFAULT_PAGE_SIZE;
    private Integer pages = 0;
    private Integer page = 1;


    public ResponsePage(Page<T> _page) {
        super(_page.getContent());
        setStatus(ResponseStatus.Success);
        setMessage(ResponseStatus.Success.toString());
        pages = _page.getTotalPages();
        elements = (int) _page.getTotalElements();
        size = _page.getSize();
        page = _page.getNumber() + 1;
    }

    public ResponsePage (Exception e){
        setStatus(ResponseStatus.Error);
        setMessage(Utils.getExceptionMessage(e));
    }

    public ResponsePage(String message) {
        setStatus(ResponseStatus.Error);
        setMessage(message);
    }

    public ResponsePage(Page<T> _page, String message) {
        super(_page.getContent());
        setMessage(message);
        setStatus(ResponseStatus.Warning);
        pages = _page.getTotalPages();
        elements = (int) _page.getTotalElements();
        size = _page.getSize();
        page = _page.getNumber() + 1;
    }

    public Page<T> convertListToPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    public static <T> Response<T> error(String message){
        return Response.error(message);
    }
    public ResponsePage(){

    }
}