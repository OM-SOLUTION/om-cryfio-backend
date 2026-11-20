package com.cryfio.common.exception;

import com.cryfio.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception){
        return error(exception.getErrorCode(),exception.getMessage(),exception.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleException(MethodArgumentNotValidException exception){
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() +": "+error.getDefaultMessage())
                .distinct()
                .toList();
        return error(ErrorCode.VALIDATION_ERROR,ErrorCode.VALIDATION_ERROR.getDefaultMessage(),errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        List<String> errors = exception
                .getConstraintViolations()
                .stream()
                .map(error ->error.getPropertyPath()+ ": "+ error.getMessage())
                .distinct()
                .toList();

        return error(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedBody(HttpMessageNotReadableException exception) {
        return error(ErrorCode.INVALID_REQUEST, "Malformed request body", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return error(ErrorCode.INVALID_REQUEST, "Invalid value for " + exception.getName(), List.of());
    }

    /*
     * Handles race conditions where two requests pass an existence
     * check but one violates a database unique constraint.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("Database integrity constraint was violated");
        return error(ErrorCode.CONFLICT,ErrorCode.CONFLICT.getDefaultMessage(),List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return error(
                ErrorCode.INVALID_REQUEST,
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception", exception);
        return error(ErrorCode.INTERNAL_ERROR,ErrorCode.INTERNAL_ERROR.getDefaultMessage(), List.of());
    }

    private ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode, String message, List<String> errors){
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getStatus().value(),errorCode.name(),message,errors));
    }

}

