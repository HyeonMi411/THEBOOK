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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

// 전역 예외 핸들러 (@RestControllerAdvice) 생성
// 컨트롤러에서 발생하는 예외를 한곳에서 가로채서 통일된 응답형태로 내려줍니다.
@RestControllerAdvice
public class GlobalExceptionHandler {

	//1. 데이터가 없을때 ( 404 Not Found)
    // 로그인 정보가 없거나 만료 → 401 (앱이 토큰을 재발급하고 다시 요청함)
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(org.springframework.security.core.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorBody("로그인이 필요합니다."));
    }

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
    // @ModelAttribute + @Valid 는 MethodArgumentNotValidException 이 아니라 BindException 이 발생.
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

    //6. 외부 API(카카오페이/카카오 도서검색/국립중앙도서관) 호출 실패 시
    // RestClient 가 4xx/5xx 응답을 받으면 HttpClientErrorException/HttpServerErrorException,
    // 네트워크 자체가 안 되면 ResourceAccessException 을 던집니다. 이걸 안 잡으면 원인을
    // 전혀 알 수 없는 밋밋한 500(Whitelabel) 으로 그대로 새어나갑니다.
    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public ResponseEntity<Map<String, String>> handleExternalApiUnauthorized(HttpClientErrorException.Unauthorized ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(createErrorBody("외부 API 인증에 실패했습니다. .env 의 API 키(KAKAO_PAY_ADMIN_KEY 등)를 확인해주세요."));
    }

    @ExceptionHandler({HttpClientErrorException.class, HttpServerErrorException.class})
    public ResponseEntity<Map<String, String>> handleExternalApiHttpError(RestClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(createErrorBody("외부 API 호출 중 오류가 발생했습니다: " + ex.getMessage()));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleExternalApiNetworkError(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(createErrorBody("외부 API 서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요."));
    }

    //7. 본인 소유가 아닌 리소스(게시글/댓글) 수정·삭제 시도 ( 403 Forbidden )
    //   PostService 의 소유자 검사에서 던지는 SecurityException 을 처리하지 않으면 500 으로 새어나간다.
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleOwnershipViolation(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorBody(ex.getMessage()));
    }

    private Map<String, String> createErrorBody(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}