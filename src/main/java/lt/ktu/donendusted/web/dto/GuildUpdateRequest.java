package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH - dalinis atnaujinimas: visi laukai neprivalomi, keiciami tik atsiusti.
 * Neatsiustas (null) laukas reiskia "palikti kaip yra", todel aprasymas niekada nedingsta.
 */
@Schema(name = "GuildUpdateRequest", description = "Dalinis grupes atnaujinimas - keiciami tik atsiusti laukai")
public record GuildUpdateRequest(

        // @NotBlank cia netinka - jis atmestu ir null (neatsiusta lauka). @Pattern null praleidzia,
        // o atsiusta reiksme privalo tureti bent viena ne tarpo simboli.
        @Schema(description = "Naujas unikalus pavadinimas; nenurodzius nekeiciamas", example = "Thursday Night Crew")
        @Pattern(regexp = ".*\\S.*", message = "Must not be blank")
        @Size(min = 3, max = 60)
        String name,

        @Schema(description = "Naujas aprasymas; nenurodzius nekeiciamas, \"\" isvalo",
                example = "Ketvirtadieniais renkasi kampanija.")
        @Size(max = 500)
        String description) {
}
