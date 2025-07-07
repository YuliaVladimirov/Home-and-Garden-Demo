package org.example.homeandgarden.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.homeandgarden.shared.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles validation errors for @Valid annotated request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, HttpServletRequest request) {

        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), errors, request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                errors,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    /**
     * Handles validation errors for @Validated annotated fields on controller level.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception, HttpServletRequest request) {

        List<String> errors = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .toList();

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), errors, request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                errors,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles errors when data is not saved in the database and can not be found .
     */
    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDataNotFoundException(DataNotFoundException exception, HttpServletRequest request) {

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles errors when provided data is already saved in the database and can not be saved again.
     */
    @ExceptionHandler(DataAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDataAlreadyExistsException(DataAlreadyExistsException exception, HttpServletRequest request) {

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles errors when the provided authentication credentials are invalid or in other credential validation scenarios.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException exception, HttpServletRequest request) {

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles errors when the application reaches an unexpected or invalid state.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(IllegalStateException  exception, HttpServletRequest request) {

        log.error("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles errors when provided Jwt has invalid format.
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwtException(MalformedJwtException exception, HttpServletRequest request) {

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles errors when provided Jwt has invalid format.
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(ExpiredJwtException exception, HttpServletRequest request) {

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles errors when provided Jwt has unsupported format.
     */
    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedJwtException(UnsupportedJwtException exception, HttpServletRequest request) {

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles errors when provided argument is illegal.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception, HttpServletRequest request) {

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
    Handles general exceptions when a conversion attempt fails.
    */
    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageConversion(HttpMessageConversionException exception, HttpServletRequest request) {

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles malformed or unreadable JSON request errors.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException exception, HttpServletRequest request) {

        Throwable cause = exception.getCause();

        if (cause instanceof InvalidFormatException) {
            handleInvalidFormatException((InvalidFormatException) cause, request);
        } else if (cause instanceof MismatchedInputException) {
            handleMismatchedInputException((MismatchedInputException) cause, request);
        }
        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles JSON parsing errors due to invalid data format.
     */
    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFormatException(InvalidFormatException exception, HttpServletRequest request) {

        String fullMessage = String.format("Invalid value for '%s': %s. Expected type: %s.",
                exception.getPath() != null && !exception.getPath().isEmpty() ? exception.getPath().getLast().getFieldName() : "unknown",
                exception.getOriginalMessage(),
                exception.getTargetType().getSimpleName());

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), fullMessage, request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                fullMessage,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles JSON parsing errors caused by structural mismatches.
     */
    @ExceptionHandler(MismatchedInputException.class)
    public ResponseEntity<ErrorResponse> handleMismatchedInputException(MismatchedInputException exception, HttpServletRequest request) {

        String fullMessage = String.format("Invalid or missing value for '%s': %s. Expected type: %s.",
                exception.getPath() != null && !exception.getPath().isEmpty() ? exception.getPath().getLast().getFieldName() : "unknown",
                exception.getOriginalMessage(),
                exception.getTargetType().getSimpleName());

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), fullMessage, request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                fullMessage,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles errors when invalid query param (path variable, request parameter or header parameter) can’t be converted to the required type
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                exception.getValue(),
                exception.getName(),
                exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "unknown");

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), message, request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                message,
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles all JSON errors.
     */
    @ExceptionHandler(JsonMappingException.class)
    public ResponseEntity<ErrorResponse> handleException(JsonMappingException exception, HttpServletRequest request) {

        log.warn("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles security errors.
     * Re-throws errors to allow Spring Security's ExceptionTranslationFilter handle these and delegate to AuthenticationEntryPoint and AccessDeniedHandler.
     */
    @ExceptionHandler({ AuthenticationException.class, AccessDeniedException.class })
    public void handleSecurityExceptions(Exception exception) throws Exception {

        log.warn("⚠️ GlobalExceptionHandler caught a security exception, re-throwing: {}", exception.getClass().getSimpleName());
        throw exception;
    }

    /**
     * Handles generic errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {

        log.error("⚠️ Error: {} | Message: {} | Endpoint: {}", exception.getClass().getSimpleName(), exception.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
