package com.cryfio.common.exception;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public class ApiException extends RuntimeException{
    private final ErrorCode errorCode;
    private final List<String> errors;
    public ApiException(ErrorCode errorCode){
        this(errorCode,errorCode.getDefaultMessage(),List.of());
    }
    public ApiException(ErrorCode errorCode,String message){
        this(errorCode,message,List.of());
    }
    public ApiException(ErrorCode errorCode,String message,List<String> errors){
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode,"Error code is required");
        this.errors = errors == null ? List.of() : List.copyOf(errors);;
    }
}
