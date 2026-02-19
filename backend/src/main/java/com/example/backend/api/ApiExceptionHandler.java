package com.example.backend.api;

import com.example.backend.api.auth.TooManyRequestsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(Instant timestamp, int status, String error, String message, List<String> details) {}

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> tooMany(TooManyRequestsException ex) {
        HttpStatus s = HttpStatus.TOO_MANY_REQUESTS; // 429
        return ResponseEntity.status(s).body(new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArg(IllegalArgumentException ex) {
        String msg = ex.getMessage() == null ? "Bad request" : ex.getMessage();
        HttpStatus s = "Invalid credentials".equalsIgnoreCase(msg) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(s).body(new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), msg, List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        HttpStatus s = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(s).body(new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), "Validation failed", details));
    }

    @ExceptionHandler(JwtEncodingException.class)
    public ResponseEntity<ApiError> jwtEncode(JwtEncodingException ex) {
        HttpStatus s = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(s).body(new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), "JWT encoding failed", List.of(ex.getMessage())));
    }
}
