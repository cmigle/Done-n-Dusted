package lt.ktu.donendusted.web;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rikiuoti leidziama tik pagal isvardintus laukus. Be sio saraso Spring Data priimtu bet kuri getteri:
 * ?sort=class baigtusi 500, o ?sort=user.passwordHash leistu is eiles spresti apie svetimus duomenis.
 */
final class Sorting {

    private Sorting() {
    }

    static Pageable allow(Pageable pageable, String... fields) {
        List<String> allowed = List.of(fields);
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot sort by \"" + order.getProperty()
                        + "\". Allowed fields: " + String.join(", ", allowed) + ".");
            }
        }
        return pageable;
    }
}
