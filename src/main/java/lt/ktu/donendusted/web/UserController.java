package lt.ktu.donendusted.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lt.ktu.donendusted.error.ApiError;
import lt.ktu.donendusted.service.UserService;
import lt.ktu.donendusted.web.dto.UserResponse;
import lt.ktu.donendusted.web.dto.UserUpdateRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Visi sio kontrolerio keliai - tik ADMIN (SecurityConfig: /api/users/** -> hasRole ADMIN). */
@RestController
@RequestMapping(path = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Naudotoju valdymas (tik ADMIN)")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "Naudotoju sarasas")
    @ApiResponse(responseCode = "200", description = "Sarasas grazintas")
    public PagedResponse<UserResponse> list(
            @Parameter(description = "Filtras pagal vardo dali") @RequestParam(required = false) String username,
            @ParameterObject @PageableDefault(size = 20, sort = "username") Pageable pageable) {

        return PagedResponse.from(
                users.list(username, Sorting.allow(pageable, "id", "username", "role", "enabled", "createdAt")),
                UserResponse::from);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Vienas naudotojas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Naudotojas rastas"),
            @ApiResponse(responseCode = "404", description = "Naudotojo nera",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public UserResponse get(@PathVariable Long userId) {
        return UserResponse.from(users.get(userId));
    }

    @PatchMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Keisti role arba uzblokuoti",
            description = "Uzblokavus atsaukiamos visos naudotojo sesijos. Savo ADMIN teisiu nuimti negalima.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Naudotojas atnaujintas"),
            @ApiResponse(responseCode = "404", description = "Naudotojo nera",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Bandoma nuimti savo ADMIN teises ar uzblokuoti save",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public UserResponse update(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest request) {
        return UserResponse.from(users.update(userId, request));
    }
}
