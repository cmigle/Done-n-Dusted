package lt.ktu.donendusted.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.List;
import lt.ktu.donendusted.error.ApiError.FieldViolation;
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

/**
 * Vienas taskas, kuriame kiekviena isimtis paverciama ApiError atsaku.
 * Kontroleriai niekada patys nekonstruoja klaidu atsakymu.
 * Spring parenka labiausiai tinkanti (siauriausia) handleri, todel Exception.class apacioje
 * pagauna tik tai, ko neapdorojo kiti - ir klientui neparodo stack trace.
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
        return build(HttpStatus.NOT_FOUND, "Tokio resurso nera: " + request.getRequestURI(), request);
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
            return unauthorized("Reikalingas prisijungimas: Authorization: Bearer <accessToken>", "Bearer", request);
        }
        String message = String.valueOf(ex.getMessage()).contains("expired")
                ? "Access zetonas pasibaiges - gaukite nauja per /api/auth/refresh"
                : "Access zetonas neteisingas";
        return unauthorized(message, "Bearer error=\"invalid_token\"", request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    /** URL taisykles (pvz. /api/users tik ADMIN) - naudotojas zinomas, bet role netinka. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Neturite teises atlikti sio veiksmo", request);
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
                    "Neteisinga reiksme - jos negalima issaugoti (pvz. neleistinas simbolis)", request);
        }
        return build(HttpStatus.CONFLICT, "Uzklausa pazeidzia duomenu vientisumo taisykle", request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    /** @Valid nepraejo uzklausos turinio (body) laukams - 400, nes payload neatitinka deklaruotos schemos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        List<FieldViolation> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Neteisingas uzklausos turinys", request, details);
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
        return build(HttpStatus.BAD_REQUEST, "Neteisingi uzklausos parametrai", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<FieldViolation> details = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Neteisingi uzklausos parametrai", request, details);
    }

    /** Netinkamas JSON, nezinoma enum reiksme uzklausos turinyje, blogas datos formatas. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        // Jackson pranesimas daugiaeilis ir su vidinemis detalemis - paliekame tik pirmaja eilute.
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause == null || cause.startsWith("Required request body is missing")) {
            // Spring pranesime butu visas kontrolerio metodo parasas - klientui jo nereikia.
            return build(HttpStatus.BAD_REQUEST, "Uzklausos turinys (JSON) privalomas", request);
        }
        if (cause.contains("Enum value out of number")) {
            return build(HttpStatus.BAD_REQUEST,
                    "Uzklausos turinio nepavyko perskaityti: enum reiksme rasoma tekstu (pvz. \"APPROVED\"), ne skaiciumi",
                    request);
        }
        String firstLine = cause.split("\\R", 2)[0];
        return build(HttpStatus.BAD_REQUEST, "Uzklausos turinio nepavyko perskaityti: " + firstLine, request);
    }

    /** Blogo tipo kelio kintamasis ar query parametras, pvz. /api/guilds/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        String expected = ex.getRequiredType() == null ? "kito tipo" : ex.getRequiredType().getSimpleName();
        return build(HttpStatus.BAD_REQUEST,
                "Parametras '" + ex.getName() + "' turi buti " + expected + ", gauta: " + ex.getValue(), request);
    }

    /** Blogai uzkoduoti query parametrai (?name=100%, ?name=%ZZ, ne UTF-8) - Tomcat juos atmeta skaitydamas. */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ApiError> handleInvalidParameters(InvalidParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Neteisingai uzkoduoti uzklausos parametrai (pvz. % reikia rasyti %25)", request);
    }

    /** Tuscias kelio kintamasis po konvertavimo, pvz. /api/guilds/%20 (tarpas vietoj ID). */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiError> handleMissingPathVariable(MissingPathVariableException ex,
                                                              HttpServletRequest request) {
        if (!ex.isMissingAfterConversion()) {
            return handleUnexpected(ex, request);
        }
        return build(HttpStatus.BAD_REQUEST,
                "Parametras '" + ex.getVariableName() + "' turi buti skaicius, gauta tuscia reiksme", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    /** Uzklausos turinys (body) atsiustas ne kaip JSON - netinkama Content-Type antraste. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Palaikomas tik application/json; gauta: " + ex.getContentType(), request);
    }

    /** Klientas Accept antrasteje prase formato, kurio API neturi. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.NOT_ACCEPTABLE, "API grazina tik application/json", request);
    }

    /** Rikiavimas pagal neegzistuojanti lauka, pvz. ?sort=bogus. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handleUnknownSortProperty(PropertyReferenceException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Rikiuoti pagal lauka '" + ex.getPropertyName() + "' negalima - tokio lauko nera", request);
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
        return build(HttpStatus.BAD_REQUEST, "Rikiuoti pagal '" + String.join("', '", sort)
                + "' negalima - nurodykite lauka ir krypti, pvz. sort=name,desc", request);
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
                    reason != null ? reason : "Neteisinga uzklausa", request);
        }
        log.error("Netiketa klaida apdorojant {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Vidine serverio klaida", request);
    }

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
