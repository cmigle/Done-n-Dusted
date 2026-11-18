package lt.ktu.donendusted.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Vieninga puslapiuoto saraso struktura su self/first/prev/next/last nuorodomis.
 * Nuorodos statomos is dabartines uzklausos URI, todel visi filtrai ir rikiavimas islieka.
 * totalElements pirmas, kad bendras kiekis (pvz. kiek is viso grupiu) matytusi atsakymo virsuje.
 */
@Schema(name = "PagedResponse", description = "Paged list with navigation links")
public record PagedResponse<T>(
        @Schema(description = "Total number of matching items (across all pages)", example = "42")
        long totalElements,
        List<T> content, PageInfo page, PageLinks links) {

    @Schema(name = "PageInfo")
    public record PageInfo(
            @Schema(example = "0") int number,
            @Schema(example = "20") int size,
            @Schema(example = "42") long totalElements,
            @Schema(example = "3") int totalPages) {
    }

    @Schema(name = "PageLinks", description = "prev and next are omitted when there is no such page")
    public record PageLinks(String self, String first, String prev, String next, String last) {
    }

    /** Konvertuoja entity puslapi i DTO puslapi ir prideda navigacijos nuorodas. */
    public static <E, D> PagedResponse<D> from(Page<E> page, Function<E, D> mapper) {
        List<D> content = page.getContent().stream().map(mapper).toList();

        int size = page.getSize();
        int current = page.getNumber();
        // Kai sarasas tuscias, totalPages = 0, todel be Math.max last butu page=-1.
        int lastIndex = Math.max(page.getTotalPages() - 1, 0);

        // Pagrindas - tas pats kelias ir visi kliento parametrai (?name=..&sort=..), jau iskoduoti.
        // Keiciami tik page ir size, todel filtrai ir rikiavimas nuorodose islieka.
        String path = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        Map<String, String[]> params = currentRequest().getParameterMap();
        PageLinks links = new PageLinks(
                link(path, params, current, size),
                link(path, params, 0, size),
                // Uz saraso ribu (page=50, kai puslapiu 3) prev rodo i paskutini esama puslapi.
                page.hasPrevious() ? link(path, params, Math.min(current - 1, lastIndex), size) : null,
                page.hasNext() ? link(path, params, current + 1, size) : null,
                link(path, params, lastIndex, size));

        return new PagedResponse<>(page.getTotalElements(), content,
                new PageInfo(current, size, page.getTotalElements(), page.getTotalPages()),
                links);
    }

    /**
     * Kiekviena reiksme koduojama lygiai viena karta. Anksciau imta jau uzkoduota kliento uzklausa
     * buvo koduojama dar karta: ?name=Iron%20Vow nuorodoje virsdavo Iron%2520Vow ir filtras nebeveikdavo.
     */
    private static String link(String path, Map<String, String[]> params, int pageNumber, int size) {
        StringJoiner query = new StringJoiner("&");
        params.forEach((name, values) -> {
            if (!name.equals("page") && !name.equals("size")) {
                for (String value : values) {
                    query.add(encode(name) + "=" + encode(value));
                }
            }
        });
        query.add("page=" + pageNumber).add("size=" + size);
        return path + "?" + query;
    }

    private static String encode(String value) {
        return UriUtils.encode(value, StandardCharsets.UTF_8);
    }

    private static HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
