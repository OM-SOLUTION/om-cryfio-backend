package com.cryfio.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
   Instant timestamp,
   boolean success,
   int status,
   String code,
   String message,
   T data,
   List<String> errors
) {
    public ApiResponse{
        errors = errors == null ? List.of() :  List.copyOf(errors);
    }
    public static <T> ApiResponse<T> success(int status,String code,String message,T data){
        return new ApiResponse<T>(Instant.now(),true,status,code,message,data,List.of());
    }
    public static ApiResponse<Void> error(int status,String code,String message,List<String> errors){
        return new ApiResponse<>(Instant.now(),false,status,code,message,null,errors);
    }
}
