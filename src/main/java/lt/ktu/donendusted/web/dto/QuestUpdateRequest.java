package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lt.ktu.donendusted.domain.Difficulty;
import lt.ktu.donendusted.domain.QuestType;
import lt.ktu.donendusted.domain.Stat;

/** PATCH - dalinis atnaujinimas: visi laukai neprivalomi, neatsiustas (null) laukas nekeiciamas. */
@Schema(name = "QuestUpdateRequest", description = "Dalinis uzduoties atnaujinimas - keiciami tik atsiusti laukai")
public record QuestUpdateRequest(

        @Schema(example = "Run 50 kilometers")
        @Pattern(regexp = ".*\\S.*", message = "Must not be blank")
        @Size(min = 3, max = 120)
        String title,

        @Schema(description = "Naujas aprasymas; nenurodzius nekeiciamas, \"\" isvalo",
                example = "Kaupiamoji uzduotis - kiekvienas patvirtintas irasas priartina prie tikslo.")
        @Size(max = 1000)
        String description,

        @Schema(example = "CON")
        Stat stat,

        @Schema(description = "Pakeitus i ne GRIND tipa, targetCount isvalomas", example = "GRIND")
        QuestType type,

        @Schema(example = "HARD")
        Difficulty difficulty,

        @Schema(description = "Leidziamas tik GRIND tipo uzduociai", example = "50")
        @Positive
        Integer targetCount,

        @Schema(example = "false")
        Boolean active) {
}
