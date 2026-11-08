package lt.ktu.donendusted.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Vieninga puslapiuoto saraso struktura su self/first/prev/next/last nuorodomis.
 * Nuorodos statomos is dabartines uzklausos URI, todel visi filtrai ir rikiavimas islieka.
 * totalElements pirmas, kad bendras kiekis (pvz. kiek is viso grupiu) matytusi atsakymo virsuje.
 */
@Schema(name = "PagedResponse", description = "Puslapiuotas sarasas su navigacijos nuorodomis")
public record PagedResponse<T>(
        @Schema(description = "Kiek is viso elementu atitinka uzklausa (visuose puslapiuose)", example = "42")
        long totalElements,
        List<T> content, PageInfo page, PageLinks links) {

    @Schema(name = "PageInfo")
    public record PageInfo(
            @Schema(example = "0") int number,
            @Schema(example = "20") int size,
            @Schema(example = "42") long totalElements,
            @Schema(example = "3") int totalPages) {
    }

    @Schema(name = "PageLinks", description = "prev ir next nera pateikiami, kai tokio puslapio nera")
    public record PageLinks(String self, String first, String prev, String next, String last) {
    }

    /** Konvertuoja entity puslapi i DTO puslapi ir prideda navigacijos nuorodas. */
    public static <E, D> PagedResponse<D> from(Page<E> page, Function<E, D> mapper) {
        List<D> content = page.getContent().stream().map(mapper).toList();

        int size = page.getSize();
        int current = page.getNumber();
        // Kai sarasas tuscias, totalPages = 0, todel be Math.max last butu page=-1.
        int lastIndex = Math.max(page.getTotalPages() - 1, 0);

        // Pagrindas - tiksliai ta URI, kuria atsiunte klientas (su ?name=..&sort=..).
        // Toliau keiciami tik page ir size, todel filtrai ir rikiavimas nuorodose islieka.
        UriComponentsBuilder base = ServletUriComponentsBuilder.fromCurrentRequest();
        PageLinks links = new PageLinks(
                link(base, current, size),
                link(base, 0, size),
                page.hasPrevious() ? link(base, current - 1, size) : null,
                page.hasNext() ? link(base, current + 1, size) : null,
                link(base, lastIndex, size));

        return new PagedResponse<>(page.getTotalElements(), content,
                new PageInfo(current, size, page.getTotalElements(), page.getTotalPages()),
                links);
    }

    private static String link(UriComponentsBuilder base, int pageNumber, int size) {
        // cloneBuilder() butinas - builder'is keiciamas vietoje, be kopijos visos nuorodos
        // gautu paskutinio kvietimo page reiksme.
        return base.cloneBuilder()
                .replaceQueryParam("page", pageNumber)
                .replaceQueryParam("size", size)
                .toUriString();
    }
}
