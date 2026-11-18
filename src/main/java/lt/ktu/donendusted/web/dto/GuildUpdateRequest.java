package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH - dalinis atnaujinimas: visi laukai neprivalomi, keiciami tik atsiusti.
 * Neatsiustas (null) laukas reiskia "palikti kaip yra", todel aprasymas niekada nedingsta.
 */
@Schema(name = "GuildUpdateRequest", description = "Partial guild update: only the fields sent are changed")
public record GuildUpdateRequest(

        // @NotBlank cia netinka - jis atmestu ir null (neatsiusta lauka). @Pattern null praleidzia,
        // o atsiusta reiksme privalo tureti bent viena ne tarpo simboli.
        @Schema(description = "New unique name; unchanged when omitted", example = "Thursday Night Crew")
        @Pattern(regexp = ".*\\S.*", message = "Must not be blank")
        @Size(min = 3, max = 60)
        String name,

        @Schema(description = "New description; unchanged when omitted, \"\" clears it",
                example = "Campaign night every Thursday.")
        @Size(max = 500)
        String description) {
}
