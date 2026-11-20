package com.cryfio.account.registration.application;

import com.cryfio.common.exception.ApiException;
import com.cryfio.common.exception.ErrorCode;

import java.util.Base64;

public final class Base64ValueDecoder {
    private Base64ValueDecoder() {}
    public static byte[] decodeExactly(String value,String fieldName,int expectedBytes){
        byte[] decoded = decode(value,fieldName);
        if(decoded.length != expectedBytes){
            throw new ApiException(ErrorCode.INVALID_REQUEST,fieldName +" must contain exactly " + expectedBytes + " bytes");
        }
        return decoded;
    }
    public static byte[] decodeAtLeast(String value,String fieldName, int minimumBytes){
        byte[] decoded = decode(value,fieldName);
        if(decoded.length < minimumBytes){
            throw new ApiException(ErrorCode.INVALID_REQUEST,fieldName + " must contain at least "+ minimumBytes + " bytes");
        }
        return decoded;
    }
    public static byte[] decode(String value,String fieldName){
        if(value == null || value.isBlank()){
            throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        String normalized = value
                .replace('-', '+')
                .replace('_', '/');
        int remainder  = normalized.length() % 4;
        if(remainder == 1){
            throw invalidBase64(fieldName);
        }
        if(remainder != 0){
            normalized += "=".repeat(4 - remainder);
        }
        try{
            return Base64.getDecoder().decode(normalized);
        }catch (IllegalArgumentException exception){
            throw invalidBase64(fieldName);
        }
    }

    private static ApiException invalidBase64(String fieldName){
        return new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " must be valid Base64 or Base64URL");
    }
}
