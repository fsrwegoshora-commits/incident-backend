package com.smartincident.incidentbackend.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseList<T> {

    private ResponseStatus status = ResponseStatus.Success;

    private List<T> data;

    private List<String> warnings = new ArrayList<>();

    private String message = "Success";

//    public  static <T> ResponseList<T> audit(List<T> data, ModuleEnums module, String auditTitle, String auditDesc) {
//        AuditTrailRepository auditTrailRepository = SpringContext.getBean(AuditTrailRepository.class);
//        if(auditTrailRepository!=null)
//            try {
//                auditTrailRepository.save(new AuditTrail(module, auditTitle, auditDesc));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        return new ResponseList<T>(data);
//    }

    public ResponseList(ResponseStatus status, List<T> data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }
    public ResponseList(ResponseStatus status, List<T> data, List<String> message) {
        this.status = status;
        this.data = data;
        this.message = String.valueOf(message);
    }

    public ResponseList(List<T> data) {
        this.data = data;
    }

    public ResponseList(String message) {
        this.status = ResponseStatus.Error;
        this.message = message;
    }

    public ResponseList (Exception e){
        status = ResponseStatus.Error;
        message = Utils.getExceptionMessage(e);
    }

    public static <T>ResponseList<T> error(String string) {
        return new ResponseList<>(string);
    }

    public static <T> ResponseList<T> warning(List<T> data, List<String> warnings, String message){
        return new ResponseList<T>(ResponseStatus.Warning, data, warnings, message);
    }

}