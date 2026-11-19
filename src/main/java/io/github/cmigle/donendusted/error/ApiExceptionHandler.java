package io.github.cmigle.donendusted.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import io.github.cmigle.donendusted.error.ApiError.FieldViolation;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Vienas taskas, kuriame kiekviena isimtis paverciama ApiError atsaku.
 * Kontroleriai niekada patys nekonstruoja klaidu atsakymu.
 * Spring parenka labiausiai tinkanti (siauriausia) handleri, todel Exception.class apacioje
 * pagauna tik tai, ko neapdorojo kiti - ir klientui neparodo stack trace.
 * Pranesimus sasaja rodo naudotojui tiesiogiai, todel juose nera Java tipu vardu ir vidiniu Jackson detaliu.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleUnknownPath(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No such endpoint: " + request.getRequestURI(), request);
    }

    /** 401 + WWW-Authenticate: klientui pasakoma, kad reikia Bearer zetono. */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return unauthorized(ex.getMessage(), "Bearer", request);
    }

    /** Is security filtru (per ApiAuthErrorHandler): zetono nera arba jis neteisingas / pasibaiges. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        if (!(ex instanceof OAuth2AuthenticationException)) {
            return unauthorized("Authentication required. Send the header Authorization: Bearer <accessToken>.",
                    "Bearer", request);
        }
        String message = String.valueOf(ex.getMessage()).contains("expired")
                ? "Access token has expired. Get a new one from /api/auth/refresh."
                : "Invalid access token";
        return unauthorized(message, "Bearer error=\"invalid_token\"", request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    /** URL taisykles (pvz. /api/users tik ADMIN) - naudotojas zinomas, bet role netinka. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Paskutine apsauga, jei lenktynes tarp dvieju uzklausu praslysta pro patikra servise (409).
     * SQLState 22xxx - pati reiksme netinka saugoti (pvz. NUL simbolis tekste), tai kliento klaida (400).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Duomenu vientisumo pazeidimas: {}", ex.getMostSpecificCause().getMessage());
        if (ex.getMostSpecificCause() instanceof SQLException sql && String.valueOf(sql.getSQLState()).startsWith("22")) {
            return build(HttpStatus.BAD_REQUEST,
                    "This value cannot be stored (for example, it contains a forbidden character).", request);
        }
        return build(HttpStatus.CONFLICT, "The request violates a data integrity rule.", request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    /**
     * @Valid nepraejo uzklausos turinio (body) laukams - 400, nes payload neatitinka deklaruotos schemos.
     * Lauku pranesimai - is src/main/resources/ValidationMessages.properties arba anotacijos message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        List<FieldViolation> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Invalid request body", request, details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException ex,
                                                              HttpServletRequest request) {
        List<FieldViolation> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldViolation(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Invalid request parameters", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<FieldViolation> details = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Invalid request parameters", request, details);
    }

    /** Netinkamas JSON, nezinomas laukas, blogas tipas, nezinoma enum reiksme, blogas datos formatas. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, unreadableBodyMessage(ex), request);
    }

    /** Blogo tipo kelio kintamasis ar query parametras, pvz. /api/guilds/abc arba ?status=BOGUS. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        Class<?> type = ex.getRequiredType();
        String name = "\"" + ex.getName() + "\"";
        String message;
        if (type != null && type.isEnum()) {
            message = "Invalid value \"" + ex.getValue() + "\" for parameter " + name + ". Allowed values: "
                    + enumValues(type) + ".";
        } else if (expectedType(type) != null) {
            message = "Parameter " + name + " must be " + expectedType(type) + ", got \"" + ex.getValue() + "\".";
        } else {
            message = "Invalid value \"" + ex.getValue() + "\" for parameter " + name + ".";
        }
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** Blogai uzkoduoti query parametrai (?name=100%, ?name=%ZZ, ne UTF-8) - Tomcat juos atmeta skaitydamas. */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ApiError> handleInvalidParameters(InvalidParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Request parameters are not URL-encoded correctly (for example, write % as %25).", request);
    }

    /** Tuscias kelio kintamasis po konvertavimo, pvz. /api/guilds/%20 (tarpas vietoj ID). */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiError> handleMissingPathVariable(MissingPathVariableException ex,
                                                              HttpServletRequest request) {
        if (!ex.isMissingAfterConversion()) {
            return handleUnexpected(ex, request);
        }
        return build(HttpStatus.BAD_REQUEST,
                "Parameter \"" + ex.getVariableName() + "\" must be a number, got an empty value.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        String[] supported = ex.getSupportedMethods();
        String message = "Method " + ex.getMethod() + " is not supported for this endpoint."
                + (supported == null || supported.length == 0 ? "" : " Allowed: " + String.join(", ", supported) + ".");
        return build(HttpStatus.METHOD_NOT_ALLOWED, message, request);
    }

    /** Uzklausos turinys (body) atsiustas ne kaip JSON - netinkama Content-Type antraste. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                               HttpServletRequest request) {
        String message = ex.getContentType() == null
                ? "Only application/json is supported. Set the Content-Type header."
                : "Only application/json is supported, got " + ex.getContentType() + ".";
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, request);
    }

    /** Klientas Accept antrasteje prase formato, kurio API neturi. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.NOT_ACCEPTABLE, "This API only returns application/json.", request);
    }

    /** Rikiavimas pagal neegzistuojanti lauka, pvz. ?sort=bogus. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handleUnknownSortProperty(PropertyReferenceException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Cannot sort by \"" + ex.getPropertyName() + "\": no such field.", request);
    }

    /**
     * Rikiavimas su skyrybos zenklais, pvz. ?sort=["string"]: Spring Data JPA tokia israiska atmeta dar
     * pries ieskodama lauko (apsauga nuo JPQL injekcijos). Kitos sios isimties priezastys - kodo klaida (500).
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiError> handleInvalidSortExpression(InvalidDataAccessApiUsageException ex,
                                                                HttpServletRequest request) {
        String[] sort = request.getParameterValues("sort");
        if (sort == null || ex.getMessage() == null || !ex.getMessage().startsWith("Sort expression")) {
            return handleUnexpected(ex, request);
        }
        return build(HttpStatus.BAD_REQUEST, "Cannot sort by \"" + String.join("\", \"", sort)
                + "\". Give a field and a direction, for example sort=name,desc.", request);
    }

    /**
     * Kitos Spring MVC kliento klaidos (ResponseStatusException, truksta parametro ir pan.) - jos pacios zino
     * savo 4xx statusa; 500 lieka tik tikroms serverio klaidoms.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            String reason = ex instanceof ResponseStatusException rse ? rse.getReason() : null;
            return build(HttpStatus.valueOf(er.getStatusCode().value()),
                    reason != null ? reason : "Bad request", request);
        }
        log.error("Netiketa klaida apdorojant {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    // ---- Uzklausos turinio (JSON) klaidos ----

    /**
     * Jackson 3 isimtis paverciama zmogui suprantamu pranesimu. Siauriausi tipai tikrinami pirmi:
     * UnrecognizedPropertyException ir InvalidFormatException yra MismatchedInputException, o
     * InputCoercionException (skaicius netelpa i int) - StreamReadException.
     */
    private static String unreadableBodyMessage(HttpMessageNotReadableException ex) {
        JacksonException jackson = jacksonCause(ex);
        if (jackson == null) {
            // Spring pranesime butu visas kontrolerio metodo parasas - klientui jo nereikia.
            return String.valueOf(ex.getMessage()).startsWith("Required request body is missing")
                    ? "Request body (JSON) is required."
                    : "Could not read the request body";
        }
        String field = fieldPath(jackson);
        String quoted = "\"" + field + "\"";
        if (jackson instanceof UnrecognizedPropertyException unknown) {
            return "Unknown field \"" + (field.isEmpty() ? unknown.getPropertyName() : field) + "\"";
        }
        if (jackson instanceof InvalidFormatException invalid && invalid.getTargetType() != null
                && invalid.getTargetType().isEnum()) {
            String values = enumValues(invalid.getTargetType());
            return invalid.getValue() instanceof Number
                    ? "Field " + quoted + " must be a string, not a number. Allowed values: " + values + "."
                    : "Invalid value \"" + invalid.getValue() + "\" for field " + quoted
                            + ". Allowed values: " + values + ".";
        }
        if (jackson instanceof MismatchedInputException mismatch) {
            if (field.isEmpty()) {
                return "Request body must be a JSON object.";
            }
            String expected = expectedType(mismatch.getTargetType());
            return expected == null
                    ? "Invalid value for field " + quoted
                    : "Field " + quoted + " must be " + expected + ".";
        }
        if (jackson instanceof InputCoercionException) {
            return field.isEmpty()
                    ? "Number is out of range."
                    : "Field " + quoted + " is out of range.";
        }
        if (jackson instanceof StreamReadException) {
            return "Malformed JSON";
        }
        log.debug("Neatpazinta JSON klaida: {}", jackson.getMessage());
        return "Could not read the request body";
    }

    /** Pirmoji Jackson isimtis priezasciu grandineje (blogos datos atveju ji pati turi DateTimeParseException priezasti). */
    private static JacksonException jacksonCause(Throwable ex) {
        for (Throwable t = ex.getCause(); t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof JacksonException jackson) {
                return jackson;
            }
        }
        return null;
    }

    /** Lauko kelias JSON dokumente, pvz. "stat" arba "items[0].name"; saknies klaidai - tuscias. */
    private static String fieldPath(JacksonException ex) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference ref : ex.getPath()) {
            if (ref.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                path.append('[').append(ref.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    /** Kokios reiksmes tikimasi - zmogui suprantamai, be Java tipu vardu (Long, Integer...). */
    private static String expectedType(Class<?> type) {
        if (type == null) {
            return null;
        }
        if (type.isEnum()) {
            return "one of: " + enumValues(type);
        }
        if (type == Boolean.class || type == boolean.class) {
            return "true or false";
        }
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != char.class)) {
            return "a number";
        }
        if (CharSequence.class.isAssignableFrom(type) || type == Character.class || type == char.class) {
            return "a string";
        }
        if (Temporal.class.isAssignableFrom(type)) {
            return "an ISO 8601 date-time, for example 2026-09-19T15:20:00Z";
        }
        if (Collection.class.isAssignableFrom(type) || type.isArray()) {
            return "an array";
        }
        return null;
    }

    private static String enumValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(value -> ((Enum<?>) value).name())
                .collect(Collectors.joining(", "));
    }

    // ---- Atsakymo kurimas ----

    private ResponseEntity<ApiError> unauthorized(String message, String wwwAuthenticate, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate)
                .body(ApiError.of(HttpStatus.UNAUTHORIZED, message, request.getRequestURI()));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        return build(status, message, request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
                                           List<FieldViolation> details) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, message, request.getRequestURI(), details));
    }
}
