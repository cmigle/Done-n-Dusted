package lt.ktu.donendusted.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Vieninga klaidos struktura - ja grazina visi API metodai, nepriklausomai nuo klaidos tipo. */
@Schema(name = "ApiError", description = "Common error response structure")
public record ApiError(

        @Schema(example = "2026-09-21T09:15:30.123Z")
        Instant timestamp,

        @Schema(example = "404")
        int status,

        @Schema(example = "Not Found")
        String error,

        @Schema(example = "Guild 99 not found")
        String message,

        @Schema(example = "/api/guilds/99")
        String path,

        @Schema(description = "Present only when the error concerns specific fields")
        List<FieldViolation> details) {

    public record FieldViolation(
            @Schema(example = "name") String field,
            @Schema(example = "Must not be blank") String message) {
    }

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ApiError of(HttpStatus status, String message, String path, List<FieldViolation> details) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path,
                details == null || details.isEmpty() ? null : details);
    }
}
