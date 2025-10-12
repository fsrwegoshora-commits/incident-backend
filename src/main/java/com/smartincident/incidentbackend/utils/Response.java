package com.smartincident.incidentbackend.utils;

import lombok.*;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Response<T> implements Serializable {

    @Builder.Default
    private ResponseStatus status = ResponseStatus.Success;

    private T data;

    @Builder.Default
    private String message = ResponseStatus.Success.toString();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();


    public Response(T data) {
        this.data = data;
        this.status = ResponseStatus.Success;
        this.message = status.toString();
    }

    public Response (Exception e){
        this.status = ResponseStatus.Error;
        message = Utils.getExceptionMessage(e);
    }

    public Response (String message) {
        this.status = ResponseStatus.Error;
        this.message = message;
    }

    public static <T> Response<T> success(T data){

        return new Response<T>(ResponseStatus.Success, data, "Success",null);
    }

    public static <T> Response<T> warning(T data, String message){
        return new Response<T>(ResponseStatus.Warning, data, message, null);
    }

    public static <T> Response<T> warning(T data, List<String> warnings){
        return new Response<T>(ResponseStatus.Warning, data, "Warning", warnings);
    }

    public static <T> Response<T> warning(T data, String message, List<String> warnings){
        return new Response<T>(ResponseStatus.Warning, data, message, warnings);
    }

    public static <T> Response<T> error(String message){
        return new Response<T>(ResponseStatus.Error, null, message,null);
    }

    public Boolean success(){
        return status.equals(ResponseStatus.Success);
    }

    public void warn(List<String> warnings){
        this.warnings = warnings;
        this.status = ResponseStatus.Warning;
    }

}
