package io.github.cmigle.donendusted.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Debesyje React sasaja (frontend/dist) supakuojama i jar kaip classpath:/static ir atiduodama is to
 * paties serverio - vienas adresas, CORS nereikia. Kelias be failo (pvz. /taverna) grazina index.html,
 * o marsrutus tvarko React Router. /api/** cia neatsiduria: neegzistuojantis API kelias lieka 404 su ApiError.
 * Vystant static katalogo nera - tada viskas veikia kaip anksciau, sasaja eina per Vite.
 */
@Configuration
public class FrontendConfig implements WebMvcConfigurer {

    private static final Resource INDEX = new ClassPathResource("static/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource file = location.createRelative(path);
                        if (file.exists() && file.isReadable()) {
                            return file;
                        }
                        boolean apiOrFile = path.startsWith("api/") || path.contains(".");
                        return !apiOrFile && INDEX.exists() ? INDEX : null;
                    }
                });
    }
}
