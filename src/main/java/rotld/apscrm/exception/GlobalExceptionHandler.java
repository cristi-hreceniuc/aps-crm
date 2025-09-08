package rotld.apscrm.exception;


import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private ProblemDetail createProblemDetail(HttpStatus status, String title, Exception ex, HttpServletRequest request) {
        log.error("Exception: {} - Title: {}", ex.getClass().getSimpleName(), title, ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("method", request.getMethod());
        return problemDetail;
    }
    @ExceptionHandler
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request.", ex, request);
    }

    @ExceptionHandler
    public ProblemDetail handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this request.", ex, request);
    }

    @ExceptionHandler
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Constraint violation.", ex, request);
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String detail = String.format("Parameter '%s' with value '%s' could not be converted to type '%s'",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());

        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid parameter type.", ex, request);
        problemDetail.setDetail(detail);
        return problemDetail;
    }

    @ExceptionHandler
    public ProblemDetail handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Endpoint not found.", ex, request);
    }

    @ExceptionHandler
    public ProblemDetail handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Wrong credentials.", ex, request);
    }

    @ExceptionHandler
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Illegal or inappropriate argument provided.", ex, request);
    }

    @ExceptionHandler
    public ProblemDetail handleEntityNotFoundException(EntityNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Entity not found.", ex, request);
    }

    @ExceptionHandler(AuthorizationServiceException.class)
    public ProblemDetail handleAuthorizationDeniedException(Exception ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.FORBIDDEN, "Access Denied.", ex, request);
    }

    @ExceptionHandler
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.CONFLICT, "Data integrity violation.", ex, request); // 409 Conflict is often more appropriate
    }

//    @ExceptionHandler
//    public ProblemDetail handlePSQLException(PSQLException ex, HttpServletRequest request) {
//        // You can add logic here to check ex.getSQLState() for specific PostgreSQL errors
//        return createProblemDetail(HttpStatus.BAD_REQUEST, "Database error.", ex, request);
//    }

    @ExceptionHandler
    public ProblemDetail handleMissingServletRequestParameterException(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Required parameter is missing.", ex, request);
    }
    @ExceptionHandler
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            if (errors.containsKey(error.getField())) {
                errors.put(error.getField(), String.format("%s, %s", errors.get(error.getField()), error.getDefaultMessage()));
            } else {
                errors.put(error.getField(), error.getDefaultMessage());
            }
        });

        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed.", ex, request);
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }
    @ExceptionHandler
    public ProblemDetail handleUncaughtException(Exception ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred.", ex, request);
    }
}