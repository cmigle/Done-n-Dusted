package io.github.cmigle.donendusted.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Puslapio numerio riba. Hibernate poslinki (page * size) laiko int, todel ?page=2147483647 ji perpildytu
 * ir baigtusi 500. Su riba 1 000 000 ir max-page-size 100 poslinkis visada telpa.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    static final long MAX_PAGE = 1_000_000;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String page = request.getParameter("page");
                if (page != null && page.trim().matches("\\d+")
                        && new BigInteger(page.trim()).compareTo(BigInteger.valueOf(MAX_PAGE)) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Page number is too large. The page parameter cannot exceed " + MAX_PAGE + ".");
                }
                return true;
            }
        });
    }
}
