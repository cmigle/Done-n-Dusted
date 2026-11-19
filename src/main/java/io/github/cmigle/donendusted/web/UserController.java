package io.github.cmigle.donendusted.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.github.cmigle.donendusted.error.ApiError;
import io.github.cmigle.donendusted.service.UserService;
import io.github.cmigle.donendusted.web.dto.UserResponse;
import io.github.cmigle.donendusted.web.dto.UserUpdateRequest;
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
@Tag(name = "Users", description = "User management (ADMIN only)")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "List users")
    @ApiResponse(responseCode = "200", description = "List returned")
    public PagedResponse<UserResponse> list(
            @Parameter(description = "Filter by part of the username") @RequestParam(required = false) String username,
            @ParameterObject @PageableDefault(size = 20, sort = "username") Pageable pageable) {

        return PagedResponse.from(
                users.list(username, Sorting.allow(pageable, "id", "username", "role", "enabled", "createdAt")),
                UserResponse::from);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public UserResponse get(@PathVariable Long userId) {
        return UserResponse.from(users.get(userId));
    }

    @PatchMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Change role or block",
            description = "Blocking revokes all of the user's sessions. You cannot remove your own ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Attempt to remove your own ADMIN role or block yourself",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public UserResponse update(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest request) {
        return UserResponse.from(users.update(userId, request));
    }
}
