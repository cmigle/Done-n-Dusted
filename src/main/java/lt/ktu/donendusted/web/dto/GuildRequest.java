package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "GuildRequest", description = "Data for creating a guild")
public record GuildRequest(

        @Schema(description = "Unique guild name", example = "Thursday Night Crew")
        @NotBlank
        @Size(min = 3, max = 60)
        String name,

        @Schema(description = "Free-form guild description; saved as \"\" when omitted",
                example = "Campaign night every Thursday.")
        @Size(max = 500)
        String description) {
}
