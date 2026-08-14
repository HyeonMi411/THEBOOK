package com.thejoa703.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 전역 예외 핸들러 (@RestControllerAdvice) 생성
// 컨트롤러에서 발생하는 예외를 한곳에서 가로채서 통일된 응답형태로 내려줍니다.
@RestControllerAdvice
public class GlobalExceptionHandler {

	//1. 데이터가 없을때 ( 404 Not Found)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorBody(ex.getMessage()));
    }

	//2. 잘못된 요청 / 비즈니스 로직오류   ( 400  Bad Request)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorBody(ex.getMessage()));
    }
    //3. @Valid 유효성 검사 실패시 (@RequestBody 방식)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    //4. @Valid 유효성 검사 실패시 (@ModelAttribute 방식, multipart/form-data 등록/수정 API)
    //   @ModelAttribute + @Valid 는 MethodArgumentNotValidException 이 아니라 BindException 이 발생합니다.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    //5. 권한 없음 (@PreAuthorize("hasRole('ADMIN')") 거부 시)  ( 403 Forbidden )
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorBody("접근 권한이 없습니다. 관리자(ROLE_ADMIN)만 이용할 수 있습니다."));
    }

    private Map<String, String> createErrorBody(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}