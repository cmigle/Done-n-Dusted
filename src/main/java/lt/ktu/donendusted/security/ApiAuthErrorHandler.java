package lt.ktu.donendusted.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Security filtrai (nera zetono, blogas zetonas, ne ADMIN) suveikia dar pries kontroleri, todel
 * ApiExceptionHandler ju nepagauna. Cia isimtis perduodama tam paciam Spring MVC mechanizmui -
 * taip ir 401/403 grazinami ta pacia ApiError struktura kaip visos kitos klaidos.
 */
@Component
public class ApiAuthErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public ApiAuthErrorHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        resolver.resolveException(request, response, null, ex);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
        resolver.resolveException(request, response, null, ex);
    }
}
