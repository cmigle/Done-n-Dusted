package lt.ktu.donendusted.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lt.ktu.donendusted.domain.User;
import lt.ktu.donendusted.error.ApiError;
import lt.ktu.donendusted.service.AuthService;
import lt.ktu.donendusted.web.dto.LoginRequest;
import lt.ktu.donendusted.web.dto.RefreshRequest;
import lt.ktu.donendusted.web.dto.RegisterRequest;
import lt.ktu.donendusted.web.dto.TokenResponse;
import lt.ktu.donendusted.web.dto.UserResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth", description = "Registracija, prisijungimas ir JWT zetonai")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(summary = "Registruotis", description = "Sukuria naudotoja su role USER. Zetonu negrazina - po to reikia prisijungti.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Naudotojas sukurtas"),
            @ApiResponse(responseCode = "400", description = "Neteisingas vardas ar slaptazodis",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Vardas jau uzimtas",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User created = auth.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/users/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(UserResponse.from(created));
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(summary = "Prisijungti", description = "Grazina trumpalaiki access zetona (JWT) ir refresh zetona.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prisijungta"),
            @ApiResponse(responseCode = "400", description = "Truksta lauku",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Neteisingas vardas arba slaptazodis",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Paskyra uzblokuota",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(summary = "Atnaujinti zetonus",
            description = """
                    Uz galiojanti refresh zetona isduoda nauja pora, o senasis atsaukiamas (rotacija).
                    Pakartotinai panaudotas zetonas laikomas pavogtu - atsaukiamos visos naudotojo sesijos.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Isduota nauja zetonu pora"),
            @ApiResponse(responseCode = "401", description = "Zetonas neteisingas, pasibaiges arba jau panaudotas",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Paskyra uzblokuota",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return auth.refresh(request.refreshToken());
    }

    @PostMapping(path = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(summary = "Atsijungti", description = "Atsaukia refresh zetona. Access zetonas galioja iki savo exp - todel jis trumpas.")
    @ApiResponse(responseCode = "204", description = "Atsijungta")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        auth.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Prisijunges naudotojas")
    @ApiResponse(responseCode = "200", description = "Naudotojo duomenys")
    public UserResponse me() {
        return UserResponse.from(auth.me());
    }
}
