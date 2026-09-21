package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "GuildRequest", description = "Grupes kurimo ir atnaujinimo duomenys")
public record GuildRequest(

        @Schema(description = "Unikalus grupes pavadinimas", example = "Thursday Night Crew")
        @NotBlank
        @Size(min = 3, max = 60)
        String name,

        @Schema(description = "Laisvas grupes aprasymas", example = "Ketvirtadieniais renkasi kampanija.")
        @Size(max = 500)
        String description) {
}
