package com.hit.userms.exception;import com.hit.userms.response.ErrorResponse;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.security.authentication.BadCredentialsException;import org.springframework.validation.FieldError;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.ExceptionHandler;import org.springframework.web.bind.annotation.RestControllerAdvice;import org.springframework.web.server.ResponseStatusException;import java.util.HashMap;import java.util.List;import java.util.Map;@RestControllerAdvicepublic class GlobalExceptionHandler {    @ExceptionHandler(BadCredentialsException.class)    public ResponseEntity<?> handleBadCredentialException(BadCredentialsException ex){        ErrorResponse errorResponse=ErrorResponse.builder()                .msg(ex.getMessage())                .status(HttpStatus.UNAUTHORIZED)                .build();        return new ResponseEntity<>(errorResponse,HttpStatus.UNAUTHORIZED);    }    @ExceptionHandler(Exception.class)    public ResponseEntity<?> handle(Exception ex){        ErrorResponse errorResponse=ErrorResponse.builder()                .msg(ex.getMessage())                .status(HttpStatus.BAD_REQUEST)                .build();        return new ResponseEntity<>(errorResponse,HttpStatus.BAD_REQUEST);    }    @ExceptionHandler(ResponseStatusException.class)    public ResponseEntity<?> handleRe(ResponseStatusException ex){        ErrorResponse errorResponse=ErrorResponse.builder()                .msg(ex.getMessage())                .status(HttpStatus.BAD_REQUEST)                .build();        return new ResponseEntity<>(errorResponse,HttpStatus.UNAUTHORIZED);    }    @ExceptionHandler(MethodArgumentNotValidException.class)    public ResponseEntity<Map<String, String>> MethodArgumentNotValidExceptionhandler(            MethodArgumentNotValidException ex) {        Map<String, String> resp = new HashMap<>();        List<FieldError> errors = ex.getBindingResult().getFieldErrors();        for (FieldError error : errors) {            String fieldName = error.getField();            String errorMessage = error.getDefaultMessage();            resp.put(fieldName, errorMessage);        }        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);    }}